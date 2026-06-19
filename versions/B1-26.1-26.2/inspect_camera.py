import zipfile

jar_path = r'C:\Users\yajae\.gradle\caches\fabric-loom\minecraftMaven\net\minecraft\minecraft-merged-deobf\26.1\minecraft-merged-deobf-26.1-sources.jar'

with zipfile.ZipFile(jar_path, 'r') as z:
    for name in z.namelist():
        if 'net/minecraft/client/Camera.java' in name:
            data = z.read(name).decode('utf-8')
            print(data)
            break
