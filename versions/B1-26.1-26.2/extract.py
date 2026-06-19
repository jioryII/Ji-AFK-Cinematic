import zipfile
import subprocess
import os

jar_path = r'C:\Users\yajae\.gradle\caches\fabric-loom\minecraftMaven\net\minecraft\minecraft-merged-deobf\26.1\minecraft-merged-deobf-26.1.jar'
class_path = 'net/minecraft/client/Camera.class'

with zipfile.ZipFile(jar_path, 'r') as z:
    with open('Camera.class', 'wb') as f:
        f.write(z.read(class_path))

# Use python bytecode disassembler (like javap, but we can download a lightweight tool or just use a python lib if available)
# Since javap is missing, let's look for it in JDK.
