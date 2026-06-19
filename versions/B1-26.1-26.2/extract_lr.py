import zipfile
import os

jar_path = r'C:\Users\yajae\.gradle\caches\fabric-loom\minecraftMaven\net\minecraft\minecraft-merged-deobf\26.1\minecraft-merged-deobf-26.1.jar'

with zipfile.ZipFile(jar_path, 'r') as z:
    for name in z.namelist():
        if 'net/minecraft/client/renderer/LevelRenderer.class' in name:
            os.makedirs('tmp_decomp', exist_ok=True)
            z.extract(name, 'tmp_decomp')
            break
