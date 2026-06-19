import os

def replace_in_file(filepath, replacements):
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()
    for old, new in replacements.items():
        content = content.replace(old, new)
    with open(filepath, 'w', encoding='utf-8') as f:
        f.write(content)

base_dir = 'c:/xampp/htdocs/Jiory-AFK-Cinematic/group-D-26.x/src/main/java/com/ji/afkcinematic/'

replace_in_file(base_dir + 'mixin/CameraMixin.java', {
    'method = "update"': 'method = "setup"',
    'method = "setPos(DDD)V"': 'method = "setPosition(DDD)V"',
    'method = "setPos"': 'method = "setPosition"'
})

replace_in_file(base_dir + 'mixin/KeyboardMixin.java', {
    'method = "onKey"': 'method = "keyPress"'
})

replace_in_file(base_dir + 'mixin/MouseMixin.java', {
    'method = "onCursorPos"': 'method = "cursorMoved"',
    'method = "onMouseButton"': 'method = "onPress"',
    'method = "onMouseScroll"': 'method = "onScroll"'
})

replace_in_file(base_dir + 'mixin/MinecraftClientMixin.java', {
    'method = "doAttack"': 'method = "startAttack"',
    'method = "doItemUse"': 'method = "startUseItem"'
})

replace_in_file(base_dir + 'mixin/PlayerNameRendererMixin.java', {
    'method = "hasLabel"': 'method = "shouldShowName"'
})

print("Fixed mixin names!")
