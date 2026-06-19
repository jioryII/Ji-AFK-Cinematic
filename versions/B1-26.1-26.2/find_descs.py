import zipfile
import re

jar_path = r'C:\Users\yajae\.gradle\caches\fabric-loom\minecraftMaven\net\minecraft\minecraft-merged-deobf\26.1\minecraft-merged-deobf-26.1.jar'

def find_descriptors(class_path, method_name):
    print(f"--- Descriptors for {method_name} in {class_path} ---")
    with zipfile.ZipFile(jar_path, 'r') as z:
        data = z.read(class_path)
    
    # Method signatures are strings like (L...)V
    # We can find the method name in the constant pool and then look for strings that start with '('
    # Or just look for any string that looks like a descriptor
    descs = re.findall(b'\\([a-zA-Z0-9_/;\\[]*\\)[a-zA-Z0-9_/;\\[]*', data)
    for d in sorted(list(set(descs))):
        print(d.decode('ascii'))

find_descriptors('net/minecraft/client/gui/screens/Screen.class', 'extractRenderState')
