import os

def replace_in_file(filepath, replacements):
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()
    for old, new in replacements.items():
        content = content.replace(old, new)
    with open(filepath, 'w', encoding='utf-8') as f:
        f.write(content)

base_dir = 'c:/xampp/htdocs/Jiory-AFK-Cinematic/group-D-26.x/src/main/java/com/ji/afkcinematic/'

replace_in_file(base_dir + 'cinematic/CameraCollisionHelper.java', {
    'direction.multiply(': 'direction.scale('
})

replace_in_file(base_dir + 'mixin/InGameHudMixin.java', {
    'import net.minecraft.client.renderer.RenderTickCounter;': 'import net.minecraft.client.DeltaTracker;',
    'RenderTickCounter tickCounter': 'DeltaTracker tickCounter'
})

replace_in_file(base_dir + 'render/LetterboxRenderer.java', {
    'import net.minecraft.client.renderer.RenderTickCounter;': 'import net.minecraft.client.DeltaTracker;'
})

replace_in_file(base_dir + 'cinematic/CinematicManager.java', {
    'client.options.smoothCameraEnabled': 'client.options.smoothCamera'
})

replace_in_file(base_dir + 'config/ConfigScreen.java', {
    'this.renderBackground(context);': 'this.renderBackground(context, 0, 0, 0.0f);',
    'int keyCode = keyInput.key();': '',
    'super.keyPressed(keyInput)': 'super.keyPressed(keyCode, scanCode, modifiers)'
})

replace_in_file(base_dir + 'music/CinematicMusicManager.java', {
    'PositionedSoundInstance.music(': 'SimpleSoundInstance.forMusic('
})

replace_in_file(base_dir + 'mixin/KeyboardMixin.java', {
    'import net.minecraft.client.Keyboard;': 'import net.minecraft.client.KeyboardHandler;'
})

replace_in_file(base_dir + 'mixin/MouseMixin.java', {
    'import net.minecraft.client.Mouse;': 'import net.minecraft.client.MouseHandler;'
})

print("Fixed!")
