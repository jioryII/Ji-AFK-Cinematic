import os
import re

mapping = {
    r'public void render\(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta\)': 'public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta)',
    r'super\.render\(context, mouseX, mouseY, delta\)': 'super.extractRenderState(context, mouseX, mouseY, delta)',
    r'drawCenteredString': 'centeredText',
    r'drawString': 'text',
    r'int keyCode = key\(\);': 'int keyCode = event.key();',
    r'0xFFFFFF': '0xFFFFFFFF',
}

def process_file(filepath):
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()
    new_content = content
    for pattern, repl in sorted(mapping.items(), key=lambda x: len(x[0]), reverse=True):
        new_content = new_content.replace(pattern.replace('\\', ''), repl)
    if new_content != content:
        with open(filepath, 'w', encoding='utf-8') as f:
            f.write(new_content)
        print(f"Updated {filepath}")

for root, _, files in os.walk('c:/xampp/htdocs/Ji-AFK-Cinematic/group-D-26.x/src/main/java'):
    for file in files:
        if file.endswith('.java'):
            process_file(os.path.join(root, file))
