import os

path = "c:/xampp/htdocs/Ji-AFK-Cinematic/versiones finales/group-D-26.x/src/main/java/com/ji/afkcinematic/config/ConfigScreen.java"

with open(path, "r", encoding="utf-8") as f:
    content = f.read()

if "private Component getActiveDisabledText" not in content:
    helper = '''
    private Component getActiveDisabledText(boolean value) {
        if (value) return Component.literal("\u00A7a").append(Component.translatable("config.ji_afkcinematic.active"));
        return Component.literal("\u00A7c").append(Component.translatable("config.ji_afkcinematic.disabled"));
    }
'''
    content = content.replace("private Component getOnOffText(boolean value) {", helper + "\n    private Component getOnOffText(boolean value) {")
    with open(path, "w", encoding="utf-8") as f:
        f.write(content)

print("Added getActiveDisabledText to ConfigScreen in Group D.")
