import zipfile
import re

jar_path = r'C:\Users\yajae\.gradle\caches\fabric-loom\minecraftMaven\net\minecraft\minecraft-merged-deobf\26.1\minecraft-merged-deobf-26.1.jar'

def inspect_class(class_path):
    print(f"--- Inspecting {class_path} ---")
    with zipfile.ZipFile(jar_path, 'r') as z:
        data = z.read(class_path)
    # Find all strings (min length 3)
    # In class files, strings are preceded by 0x01 (tag) and length (2 bytes)
    # We can just look for byte sequences that look like ASCII
    matches = re.findall(b'[a-zA-Z0-9_/]{3,}', data)
    found = set()
    for m in matches:
        s = m.decode('ascii', errors='ignore')
        if any(x in s.lower() for x in ['render', 'draw', 'text', 'background', 'fill', 'state', 'centered']):
            found.add(s)
    for s in sorted(list(found)):
        print(s)

inspect_class('net/minecraft/client/gui/screens/Screen.class')
inspect_class('net/minecraft/client/gui/GuiGraphicsExtractor.class')
