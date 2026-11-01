"""Compile all supported native adapters and assemble the twelve release groups."""
from pathlib import Path
import argparse, hashlib, importlib.util, json, os, shutil, subprocess, urllib.request
ROOT = Path(__file__).resolve().parents[1]
def sha(path): return hashlib.sha256(path.read_bytes()).hexdigest()
def gradle(project, args, offline):
    wrapper = project / ('gradlew.bat' if os.name == 'nt' else 'gradlew')
    command = [str(wrapper)] if os.name == 'nt' else ['sh', str(wrapper)]
    if os.environ.get('JAVA21_HOME'):
        args = args + ['-Porg.gradle.java.installations.paths=' + os.environ['JAVA21_HOME'] + ',' + os.environ['JAVA_HOME']]
    subprocess.run(command + args + (['--offline'] if offline else []) + ['--no-daemon'], cwd=project, check=True)
def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--java21-home', type=Path, default=os.environ.get('JAVA21_HOME'))
    parser.add_argument('--resume', action='store_true', help='Continue an interrupted build without changing the sources')
    parser.add_argument('--offline', action='store_true')
    parser.add_argument('--output', default='release-2.4.0')
    args = parser.parse_args()
    if args.java21_home: os.environ['JAVA21_HOME'] = str(args.java21_home.resolve())
    if not os.environ.get('JAVA_HOME'): parser.error('Set JAVA_HOME to a JDK 25 installation; also install JDK 21 for 1.21.x toolchains.')
    if Path(args.output).name != args.output: parser.error('Output must be a directory name')
    out = ROOT / 'build/grouped' / args.output
    if out.exists(): parser.error('Preserve the existing grouped output; select another --output name')
    registry = json.loads((ROOT / 'build-targets.json').read_text())
    source = ROOT / 'build/exact-products'; jars = source / 'jars'; jars.mkdir(parents=True, exist_ok=True)
    gradle(ROOT, ['prepareGroupedToolchain'], args.offline)
    annotation = ROOT / 'build/grouped-toolchain/mergetool-api.jar'
    expected = '0260881294d8b218afe3d673f084d3745a55c28b35205d07d5e62d7f22202a00'
    if not annotation.exists():
        annotation.parent.mkdir(parents=True, exist_ok=True)
        cache = Path(os.environ.get('GRADLE_USER_HOME', str(Path.home() / '.gradle'))) / 'caches/modules-2/files-2.1'
        cached = next(cache.glob('net.neoforged/mergetool/2.0.0/*/*-api.jar'), None)
        if cached is not None: shutil.copyfile(cached, annotation)
        elif args.offline: raise FileNotFoundError('Run once online to cache the NeoForge annotation API')
        else:
            url = 'https://maven.neoforged.net/releases/net/neoforged/mergetool/2.0.0/mergetool-2.0.0-api.jar'
            with urllib.request.urlopen(url, timeout=60) as response: annotation.write_bytes(response.read())
    if sha(annotation) != expected: raise RuntimeError('NeoForge annotation API hash mismatch')
    products = []
    prior = {}
    checkpoint = source / 'products.json'
    if args.resume and checkpoint.exists():
        for product in json.loads(checkpoint.read_text())['products']:
            path = jars / product['filename']
            if not path.is_file() or sha(path) != product['sha256']: raise RuntimeError('Changed checkpoint artifact')
            prior[(product['loader'], product['minecraft'])] = product
    for group in registry['targets']:
        loader = group['loader']; release = '2.4.0' if loader == 'fabric' else '2.4.0-beta.1.1'
        for mc in group['minecraftVersions']:
            if (loader, mc) in prior:
                products.append(prior[(loader, mc)])
                print('RESUME', loader, mc, flush=True)
                continue
            print('BUILD', loader, mc, flush=True)
            project = ROOT / 'versions' / group['id']
            gradle(project, ['remapJar' if loader == 'fabric' and mc.startswith('1.21') else 'jar', '-Pminecraft_version=' + mc, '-Pmod_version=' + release], args.offline)
            name = 'ji-afk-cinematic-' + loader + '-' + mc + '-' + release + '.jar'
            src = project / 'build/libs' / name
            if not src.is_file(): raise FileNotFoundError('Missing exact production JAR: ' + str(src))
            shutil.copyfile(src, jars / name)
            products.append({'loader': loader, 'minecraft': mc, 'filename': name, 'sha256': sha(jars / name)})
            (source / 'products.json').write_text(json.dumps({'products': products}, indent=2))
    spec = importlib.util.spec_from_file_location('group_artifacts', ROOT / 'tools/group_artifacts.py')
    assembly = importlib.util.module_from_spec(spec); spec.loader.exec_module(assembly)
    import sys
    sys.argv = ['group_artifacts', args.output]; assembly.main()
    manifest = json.loads((out / 'manifest.json').read_text())
    delivery = ROOT / 'compilaciones'; delivery.mkdir(exist_ok=True)
    history = ROOT / 'build/previous-artifacts'; history.mkdir(parents=True, exist_ok=True)
    for product in manifest['products']:
        src = out / product['file']; target = delivery / product['file']
        if target.exists() and sha(target) != product['sha256']:
            backup = history / (sha(target) + '-' + target.name)
            if not backup.exists(): shutil.copyfile(target, backup)
        shutil.copyfile(src, target)
        if sha(target) != product['sha256']: raise RuntimeError('Delivery hash mismatch: ' + target.name)
    print('Built twelve grouped JARs in', delivery)
if __name__ == '__main__': main()
