import zipfile
import glob

jar_paths = glob.glob(r'C:\Users\yajae\.gradle\caches\fabric-loom\minecraftMaven\net\minecraft\minecraft-merged-deobf\26.1\*.jar')
jar_path = jar_paths[0]

with zipfile.ZipFile(jar_path, 'r') as z:
    for f in z.namelist():
        if 'Camera' in f or 'Entity' in f:
            # just check if there's a good entity to use
            pass
