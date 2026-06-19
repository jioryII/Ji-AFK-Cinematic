import os
import zipfile
import re

jar_path = r'C:\Users\yajae\.gradle\caches\fabric-loom\minecraftMaven\net\minecraft\minecraft-merged-deobf\26.1\minecraft-merged-deobf-26.1.jar'
with zipfile.ZipFile(jar_path, 'r') as z:
    for name in z.namelist():
        if name.endswith('.class'):
            data = z.read(name)
            if b'fill' in data:
                print(name)
