"""Assemble the twelve delivery groups. No game launches or test runners.

NeoForge variants use private Java packages and shader resource paths; only the
matching version contributes mixins and constructs the original client entrypoint.
Fabric keeps exact jars nested so its dependency solver selects by Minecraft.
"""
from pathlib import Path
from zipfile import ZipFile, ZipInfo, ZIP_DEFLATED
import hashlib
import json
import re
import shutil
import subprocess
import sys
import os
import tomllib

ROOT = Path(__file__).resolve().parents[1]
HERE = ROOT / 'build/grouped'
HERE.mkdir(parents=True, exist_ok=True)
SOURCE = ROOT / 'build/exact-products'
CACHE = Path(os.environ.get('GRADLE_USER_HOME', str(Path.home() / '.gradle'))) / 'caches/modules-2/files-2.1'
JDK = Path(os.environ['JAVA_HOME']) / 'bin'
PREFIX = 'com/ji/afkcinematic/'
DOT = PREFIX.replace('/', '.')
BOOT = 'com.ji.afkcinematic.group.'


def sha(data):
    return hashlib.sha256(data).hexdigest()


def jar_entries(raw):
    import io
    with ZipFile(io.BytesIO(raw)) as jar:
        names = [n for n in jar.namelist() if not n.endswith('/')]
        if len(names) != len(set(names)):
            raise ValueError('Duplicate ZIP entry')
        return {n: jar.read(n) for n in names}


def write_jar(path, entries):
    with ZipFile(path, 'x', ZIP_DEFLATED, compresslevel=9) as jar:
        for name, data in sorted(entries.items()):
            info = ZipInfo(name, (2026, 10, 31, 0, 0, 0))
            info.compress_type = ZIP_DEFLATED
            jar.writestr(info, data, compresslevel=9)


def rewrite_class(data, replacements):
    """Rewrite ASCII portions of modified-UTF8 constants without decoding them."""
    if data[:4] != bytes.fromhex('cafebabe'):
        raise ValueError('Invalid class header')
    result = bytearray(data[:10])
    pos, index = 10, 1
    widths = {3: 4, 4: 4, 5: 8, 6: 8, 7: 2, 8: 2, 9: 4, 10: 4,
              11: 4, 12: 4, 15: 3, 16: 2, 17: 4, 18: 4, 19: 2, 20: 2}
    while index < int.from_bytes(data[8:10], 'big'):
        tag = data[pos]
        result.append(tag)
        pos += 1
        if tag == 1:
            size = int.from_bytes(data[pos:pos + 2], 'big')
            value = data[pos + 2:pos + 2 + size]
            for old, new in replacements:
                value = value.replace(old.encode('ascii'), new.encode('ascii'))
            result += len(value).to_bytes(2, 'big') + value
            pos += size + 2
        else:
            size = widths[tag]
            result += data[pos:pos + size]
            pos += size
            if tag in (5, 6):
                index += 1
        index += 1
    return bytes(result) + data[pos:]


def dependency_range(meta, mod):
    return next(d['versionRange'] for d in meta['dependencies']['ji_afk_cinematic'] if d['modId'] == mod)


def bounded_neo_range(minimum):
    if not minimum.startswith('[') or not minimum.endswith(',)'):
        raise ValueError('Unexpected NeoForge range: ' + minimum)
    version = minimum[1:-2]
    pieces = version.split('.')
    # 21.x loaders use minor = Minecraft patch; 26.x use third = patch.
    count = 3 if pieces[0] == '26' else 2
    boundary = pieces[:count]
    boundary[-1] = str(int(boundary[-1]) + 1)
    return '[' + version + ',' + '.'.join(boundary) + ')'


def compile_tools(work):
    def cached(pattern):
        return next(p for p in CACHE.glob(pattern) if not p.name.endswith(('-sources.jar', '-javadoc.jar'))).resolve()
    asm = cached('org.ow2.asm/asm/9.10.1/*/*.jar')
    deps = [asm,
            cached('org.ow2.asm/asm-tree/9.10.1/*/*.jar'),
            cached('net.fabricmc/sponge-mixin/0.15.4+mixin.0.8.7/*/*.jar'),
            cached('net.neoforged.fancymodloader/loader/4.0.44/*/*.jar'),
            cached('net.neoforged/bus/8.0.2/*/*.jar'),
            ROOT / 'build/grouped-toolchain/mergetool-api.jar']
    classes = work / 'assembly-classes'
    classes.mkdir()
    subprocess.run([str(JDK / ('javac.exe' if os.name == 'nt' else 'javac')), '-proc:none', '--release', '21', '-cp', os.pathsep.join(map(str, deps)),
                    '-d', str(classes), *map(str, sorted((ROOT / 'gradle/grouped-src').glob('*.java')))], check=True)
    runtime = {p.relative_to(classes).as_posix(): p.read_bytes()
               for p in classes.rglob('*.class') if p.relative_to(classes).as_posix().startswith(PREFIX)}
    return classes, asm, runtime


def neo_group(group, inputs, work, runtime, classes, asm):
    versions = group['minecraftVersions']
    if len(versions) == 1:
        return inputs[0][2], {'strategy': 'exact-single-version'}
    all_entries = [jar_entries(raw) for _, _, raw in inputs]
    skip = {'META-INF/neoforge.mods.toml', 'ji-afk-build-id.txt'}
    # Groups whose implementation is already shared need no runtime selector.
    canonical = {k: v for k, v in all_entries[0].items() if k not in skip}
    identical = all({k: v for k, v in e.items() if k not in skip} == canonical for e in all_entries[1:])
    metas = [tomllib.loads(e['META-INF/neoforge.mods.toml'].decode()) for e in all_entries]
    metadata = all_entries[0]['META-INF/neoforge.mods.toml'].decode()
    metadata = metadata.replace('versionRange="' + dependency_range(metas[0], 'minecraft') + '"',
                                'versionRange="' + ','.join('[' + v + ']' for v in versions) + '"')
    neo_range = ','.join(bounded_neo_range(dependency_range(m, 'neoforge')) for m in metas)
    metadata = metadata.replace('versionRange="' + dependency_range(metas[0], 'neoforge') + '"',
                                'versionRange="' + neo_range + '"')
    audit = {'strategy': 'shared-identical-implementation' if identical else 'isolated-version-implementations',
             'variants': [], 'neoforgeRange': neo_range}
    if identical:
        result = dict(all_entries[0])
    else:
        result = dict(runtime)
        metadata = re.sub(r'\[\[mixins\]\]\s*config="[^"]+"\s*', '', metadata)
        variant_properties = []
        strip_paths = []
        config_names = []
        for (version, product, raw), entries, meta in zip(inputs, all_entries, metas):
            key = 'v' + version.replace('.', '_')
            private_prefix = PREFIX + 'variants/' + key + '/'
            private_dot = private_prefix.replace('/', '.')
            replacements = [(PREFIX, private_prefix), (DOT, private_dot),
                            ('core/focus_', 'core/' + key + '/focus_'),
                            ('pipeline/focus_', 'pipeline/' + key + '/focus_'),
                            ('ji_afk_focus', key + '_ji_afk_focus'),
                            ('ji-afk-cinematic:focus.glsl', 'ji-afk-cinematic:' + key + '/focus.glsl')]
            shaders = []
            for name, data in entries.items():
                if name in skip or name.endswith('mixins.json'):
                    continue
                if name.startswith(PREFIX):
                    if not name.endswith('.class'):
                        raise ValueError('Unhandled private resource: ' + name)
                    dest = private_prefix + name[len(PREFIX):]
                    data = rewrite_class(data, replacements)
                    if name == PREFIX + 'platform/NeoForgeClientEntrypoint.class':
                        strip = work / ('entrypoint-' + group['id'] + '-' + key + '.class')
                        strip.write_bytes(data)
                        strip_paths.append((strip, dest))
                    else:
                        result[dest] = data
                    continue
                dest = name
                if '/shaders/' in name:
                    for old, new in replacements[2:]:
                        dest = dest.replace(old, new)
                        data = data.replace(old.encode(), new.encode())
                    if '/shaders/include/focus.glsl' in dest:
                        dest = dest.replace('/shaders/include/focus.glsl', '/shaders/include/' + key + '/focus.glsl')
                    shaders.append(dest)
                if dest in result and result[dest] != data:
                    raise ValueError('Unresolved shared resource: ' + dest + ' in ' + version)
                result[dest] = data
            for declaration in meta.get('mixins', []):
                original = declaration['config']
                config = json.loads(entries[original])
                package = config['package'].replace(DOT, private_dot)
                names = config.get('mixins', []) + config.get('client', [])
                if config.get('server') or config.get('refmap'):
                    raise ValueError('Unhandled mixin mode: ' + original)
                for name in names:
                    if package.replace('.', '/') + '/' + name.replace('.', '/') + '.class' not in result:
                        raise ValueError('Missing variant mixin: ' + package + '.' + name)
                props = ['minecraft=' + version, 'mixins=' + ','.join(names),
                         'delegate=' + config.get('plugin', '').replace(DOT, private_dot)]
                result['META-INF/ji-afk-variants/' + package + '.properties'] = ('\n'.join(props) + '\n').encode()
                config.update({'package': package, 'plugin': BOOT + 'VariantMixinPlugin', 'client': [], 'mixins': []})
                config_name = key + '-' + original
                result[config_name] = (json.dumps(config, indent=2) + '\n').encode()
                config_names.append(config_name)
            variant_properties.append(version + '=' + private_dot + 'platform.NeoForgeClientEntrypoint')
            audit['variants'].append({'minecraft': version, 'package': private_dot, 'shaderResources': shaders,
                                      'classCount': sum(n.endswith('.class') for n in entries)})
        subprocess.run([str(JDK / ('java.exe' if os.name == 'nt' else 'java')), '-cp', str(classes) + os.pathsep + str(asm), 'StripModAnnotation',
                        *[str(p) for p, _ in strip_paths]], check=True)
        for path, dest in strip_paths:
            result[dest] = path.read_bytes()
        result['META-INF/ji-afk-group.properties'] = ('\n'.join(variant_properties) + '\n').encode()
        metadata += '\n' + '\n'.join('[[mixins]]\nconfig="' + n + '"\n' for n in config_names)
    result['META-INF/neoforge.mods.toml'] = metadata.encode()
    result['ji-afk-build-id.txt'] = ('group-source-sha256:' + ','.join(p['sha256'] for _, p, _ in inputs) + '\n').encode()
    return result, audit


def main():
    if len(sys.argv) != 2:
        raise SystemExit('Usage: py ensamblar-grupos.py <new-output-name>')
    out = HERE / sys.argv[1]
    if out.exists():
        raise SystemExit('Preserve previous assembly: ' + str(out))
    out.mkdir()
    classes, asm, runtime = compile_tools(out)
    delivery = json.loads((SOURCE / 'products.json').read_text(encoding='utf-8'))
    products = {(p['loader'], p['minecraft']): p for p in delivery['products']}
    registry = json.loads((ROOT / 'build-targets.json').read_text(encoding='utf-8'))
    rows = []
    for group in registry['targets']:
        versions, loader = group['minecraftVersions'], group['loader']
        inputs = []
        for version in versions:
            product = products[(loader, version)]
            raw = (SOURCE / 'jars' / product['filename']).read_bytes()
            if sha(raw) != product['sha256']:
                raise ValueError('Changed source jar: ' + product['filename'])
            inputs.append((version, product, raw))
        version_string = '2.4.0' if loader == 'fabric' else '2.4.0-beta.1.1'
        mc_name = versions[0] if len(versions) == 1 else versions[0] + '-' + versions[-1]
        filename = 'ji-afk-cinematic-' + loader + '-' + mc_name + '-' + version_string + '.jar'
        if loader == 'neoforge':
            result, audit = neo_group(group, inputs, out, runtime, classes, asm)
        elif len(versions) == 1:
            result, audit = inputs[0][2], {'strategy': 'exact-single-version'}
        else:
            original = jar_entries(inputs[0][2])
            metadata = json.loads(original['fabric.mod.json'])
            metadata.update({'id': 'ji_afk_cinematic_group', 'version': version_string,
                             'entrypoints': {}, 'mixins': [],
                             'depends': {'minecraft': versions, 'ji-afk-cinematic': '=' + version_string},
                             'jars': [{'file': 'META-INF/jars/' + p['filename']} for _, p, _ in inputs]})
            metadata.setdefault('custom', {})['modmenu'] = {
                'badges': ['library'], 'parent': 'ji-afk-cinematic',
                'update_checker': False}
            for field in ('accessWidener', 'provides', 'breaks', 'conflicts', 'recommends', 'suggests'):
                metadata.pop(field, None)
            result = {'fabric.mod.json': (json.dumps(metadata, indent=2, ensure_ascii=False) + '\n').encode(),
                      'LICENSE': original['LICENSE']}
            if metadata.get('icon') in original:
                result[metadata['icon']] = original[metadata['icon']]
            for v, p, raw in inputs:
                nested_meta = json.loads(jar_entries(raw)['fabric.mod.json'])
                if nested_meta['depends']['minecraft'] != v or nested_meta['id'] != 'ji-afk-cinematic':
                    raise ValueError('Unexpected Fabric input')
                result['META-INF/jars/' + p['filename']] = raw
            audit = {'strategy': 'fabric-loader-nested-exact-selection'}
        dest = out / filename
        if isinstance(result, bytes):
            dest.write_bytes(result)
        else:
            write_jar(dest, result)
        # Assembly integrity only: hashes/metadata/resource consistency, no game or test suite.
        packaged = jar_entries(dest.read_bytes())
        if loader == 'neoforge':
            tomllib.loads(packaged['META-INF/neoforge.mods.toml'].decode())
        rows.append({'group': group['id'], 'loader': loader, 'minecraftVersions': versions,
                     'version': version_string, 'file': filename, 'sha256': sha(dest.read_bytes()),
                     'bytes': dest.stat().st_size, **audit,
                     'inputs': [{'minecraft': v, 'file': p['filename'], 'sha256': p['sha256']} for v, p, _ in inputs]})
        print(group['id'], filename, flush=True)
    (out / 'manifest.json').write_text(json.dumps({'status': 'ASSEMBLED', 'testsRun': False,
        'runtimeValidation': 'NOT_RUN_PER_USER_INSTRUCTION', 'products': rows}, indent=2, ensure_ascii=False) + '\n', encoding='utf-8')
    print('Assembled', len(rows), 'groups. No tests run.')


if __name__ == '__main__':
    main()
