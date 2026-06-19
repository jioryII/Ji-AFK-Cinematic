import zipfile
import subprocess
import os

jar_path = r'C:\Users\yajae\.gradle\caches\fabric-loom\minecraftMaven\net\minecraft\minecraft-merged-deobf\26.1\minecraft-merged-deobf-26.1.jar'
class_path = 'net/minecraft/client/renderer/SectionOcclusionGraph.class'

with zipfile.ZipFile(jar_path, 'r') as z:
    with open('SectionOcclusionGraph.class', 'wb') as f:
        f.write(z.read(class_path))

