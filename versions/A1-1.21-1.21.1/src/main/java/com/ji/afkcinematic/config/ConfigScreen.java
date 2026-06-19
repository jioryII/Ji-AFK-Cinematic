package com.ji.afkcinematic.config;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.client.gui.tooltip.Tooltip;

import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

public class ConfigScreen extends Screen {
    private final Screen parent;
    private ModConfig editConfig;
    private boolean waitingForKey = false;
    private ButtonWidget keyBindButton;
    private ButtonWidget reportButton;
    private static final Identifier LOGO_TEXTURE = Identifier.of("ji-afk-cinematic", "icon.png");

    public ConfigScreen(Screen parent) {
        super(Text.translatable("config.ji_afkcinematic.title"));
        this.parent = parent;
        cloneConfig();
    }

    private void cloneConfig() {
        ModConfig current = ConfigManager.getConfig();
        this.editConfig = new ModConfig();
        this.editConfig.shotDurationSeconds = current.shotDurationSeconds;
        this.editConfig.afkThresholdSeconds = current.afkThresholdSeconds;
        this.editConfig.maxCycles = current.maxCycles;
        this.editConfig.cameraSpeed = current.cameraSpeed;
        this.editConfig.damageAction = current.damageAction;
        this.editConfig.extendedMusic = current.extendedMusic;
        this.editConfig.modEnabled = current.modEnabled;
        this.editConfig.enableLetterbox = current.enableLetterbox;
        this.editConfig.enableMusic = current.enableMusic;
        this.editConfig.configKeyCode = current.configKeyCode;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int yLeft = 85;
        int yRight = 85;
        int widgetWidth = 135;
        int entryHeight = 26;
        int infoWidth = 20;

        int col1X = centerX - 140;
        int col2X = centerX + 5;

        // --- Left Column ---
        
        // Shot Duration slider
        this.addDrawableChild(new SliderWidget(
                col1X, yLeft, widgetWidth, 20,
                Text.translatable("config.ji_afkcinematic.shot_duration", editConfig.shotDurationSeconds),
                (editConfig.shotDurationSeconds - 5.0) / 55.0
        ) {
            @Override
            protected void updateMessage() {
                int val = 5 + (int) (this.value * 55);
                this.setMessage(Text.translatable("config.ji_afkcinematic.shot_duration", val));
            }
            @Override
            protected void applyValue() {
                editConfig.shotDurationSeconds = 5 + (int) (this.value * 55);
            }
            { setTooltip(Tooltip.of(Text.translatable("config.ji_afkcinematic.tooltip.shot_duration"))); }
        });
        yLeft += entryHeight;

        // AFK Threshold slider
        this.addDrawableChild(new SliderWidget(
                col1X, yLeft, widgetWidth, 20,
                Text.translatable("config.ji_afkcinematic.afk_threshold", editConfig.afkThresholdSeconds),
                (editConfig.afkThresholdSeconds - 10.0) / 590.0
        ) {
            @Override
            protected void updateMessage() {
                int val = 10 + (int) (this.value * 590);
                this.setMessage(Text.translatable("config.ji_afkcinematic.afk_threshold", val));
            }
            @Override
            protected void applyValue() {
                editConfig.afkThresholdSeconds = 10 + (int) (this.value * 590);
            }
            { setTooltip(Tooltip.of(Text.translatable("config.ji_afkcinematic.tooltip.afk_threshold"))); }
        });
        yLeft += entryHeight;

        // Max Cycles slider
        this.addDrawableChild(new SliderWidget(
                col1X, yLeft, widgetWidth, 20,
                Text.translatable("config.ji_afkcinematic.max_cycles", editConfig.maxCycles),
                (editConfig.maxCycles - 1.0) / 19.0
        ) {
            @Override
            protected void updateMessage() {
                int val = 1 + (int) (this.value * 19);
                this.setMessage(Text.translatable("config.ji_afkcinematic.max_cycles", val));
            }
            @Override
            protected void applyValue() {
                editConfig.maxCycles = 1 + (int) (this.value * 19);
            }
            { setTooltip(Tooltip.of(Text.translatable("config.ji_afkcinematic.tooltip.max_cycles"))); }
        });
        yLeft += entryHeight;

        // Camera Speed slider
        this.addDrawableChild(new SliderWidget(
                col1X, yLeft, widgetWidth, 20,
                Text.translatable("config.ji_afkcinematic.camera_speed", String.format("%.1f", editConfig.cameraSpeed)),
                (editConfig.cameraSpeed - 0.1) / 2.9
        ) {
            @Override
            protected void updateMessage() {
                float val = 0.1f + (float) (this.value * 2.9);
                this.setMessage(Text.translatable("config.ji_afkcinematic.camera_speed", String.format("%.1f", val)));
            }
            @Override
            protected void applyValue() {
                editConfig.cameraSpeed = 0.1f + (float) (this.value * 2.9);
            }
            { setTooltip(Tooltip.of(Text.translatable("config.ji_afkcinematic.tooltip.camera_speed"))); }
        });
        yLeft += entryHeight;

        

        // --- Right Column ---
        
        // Damage Action Button
        this.addDrawableChild(ButtonWidget.builder(
                getDamageActionText(),
                button -> {
                    int nextOrdinal = (editConfig.damageAction.ordinal() + 1) % DamageAction.values().length;
                    editConfig.damageAction = DamageAction.values()[nextOrdinal];
                    button.setMessage(getDamageActionText());
                }
        ).tooltip(Tooltip.of(Text.translatable("config.ji_afkcinematic.tooltip.damage_action"))).dimensions(col2X, yRight, widgetWidth, 20).build());
        yRight += entryHeight;

        
        // Letterbox toggle
        this.addDrawableChild(ButtonWidget.builder(
                Text.translatable("config.ji_afkcinematic.letterbox").append(": ").append(getOnOffText(editConfig.enableLetterbox)),
                button -> {
                    editConfig.enableLetterbox = !editConfig.enableLetterbox;
                    button.setMessage(Text.translatable("config.ji_afkcinematic.letterbox").append(": ").append(getOnOffText(editConfig.enableLetterbox)));
                }
        ).tooltip(Tooltip.of(Text.translatable("config.ji_afkcinematic.tooltip.letterbox"))).dimensions(col2X, yRight, widgetWidth, 20).build());
        yRight += entryHeight;

        // Music toggle
        this.addDrawableChild(ButtonWidget.builder(
                Text.translatable("config.ji_afkcinematic.music").append(": ").append(getOnOffText(editConfig.enableMusic)),
                button -> {
                    editConfig.enableMusic = !editConfig.enableMusic;
                    button.setMessage(Text.translatable("config.ji_afkcinematic.music").append(": ").append(getOnOffText(editConfig.enableMusic)));
                }
        ).tooltip(Tooltip.of(Text.translatable("config.ji_afkcinematic.tooltip.music"))).dimensions(col2X, yRight, widgetWidth, 20).build());
        yRight += entryHeight;

        // Extended Music toggle
        this.addDrawableChild(ButtonWidget.builder(
                Text.translatable("config.ji_afkcinematic.extended_music").append(": ").append(getOnOffText(editConfig.extendedMusic)),
                button -> {
                    editConfig.extendedMusic = !editConfig.extendedMusic;
                    button.setMessage(Text.translatable("config.ji_afkcinematic.extended_music").append(": ").append(getOnOffText(editConfig.extendedMusic)));
                }
        ).tooltip(Tooltip.of(Text.translatable("config.ji_afkcinematic.tooltip.extended_music"))).dimensions(col2X, yRight, widgetWidth, 20).build());
        yRight += entryHeight;

        

        

        // --- Center Section ---
        int centerStartY = Math.max(yLeft, yRight) + 5;
        
        // Mod Enabled toggle
        this.addDrawableChild(ButtonWidget.builder(
                Text.translatable("config.ji_afkcinematic.enabled").append(": ").append(getActiveDisabledText(editConfig.modEnabled)),
                button -> {
                    editConfig.modEnabled = !editConfig.modEnabled;
                    button.setMessage(Text.translatable("config.ji_afkcinematic.enabled").append(": ").append(getActiveDisabledText(editConfig.modEnabled)));
                }
        ).tooltip(Tooltip.of(Text.translatable("config.ji_afkcinematic.tooltip.enabled"))).dimensions(centerX - widgetWidth / 2, centerStartY, widgetWidth, 20).build());
        centerStartY += entryHeight;

        // Config Keybind button
        keyBindButton = ButtonWidget.builder(
                Text.translatable("config.ji_afkcinematic.keybind", getKeyName(editConfig.configKeyCode)),
                button -> {
                    waitingForKey = true;
                    button.setMessage(Text.translatable("config.ji_afkcinematic.keybind_waiting"));
                }
        ).tooltip(Tooltip.of(Text.translatable("config.ji_afkcinematic.tooltip.keybind"))).dimensions(centerX - widgetWidth / 2, centerStartY, widgetWidth, 20).build();
        this.addDrawableChild(keyBindButton);

        // --- Bottom Centered Buttons ---
        
        // Report Button
        this.reportButton = ButtonWidget.builder(
                Text.literal("\u00A7e\u26A0"),
                button -> { net.minecraft.util.Util.getOperatingSystem().open("https://discord.gg/sE27D5SNaq"); }
        ).tooltip(Tooltip.of(Text.translatable("config.ji_afkcinematic.tooltip.report"))).dimensions(this.width - 35, this.height - 35, 30, 30).build();
        this.addDrawableChild(this.reportButton);

        int bottomY = this.height - 35;
        
        // Reset to Defaults button
        this.addDrawableChild(ButtonWidget.builder(
                Text.translatable("config.ji_afkcinematic.reset_defaults"),
                button -> {
                    this.editConfig = new ModConfig();
                    this.clearAndInit();
                }
        ).dimensions(centerX - 155, bottomY, 100, 20).build());

        // Save & Done button
        this.addDrawableChild(ButtonWidget.builder(
                Text.translatable("config.ji_afkcinematic.save"),
                button -> {
                    editConfig.recalculate();
                    ConfigManager.setConfig(editConfig);
                    this.close();
                }
        ).dimensions(centerX - 50, bottomY, 100, 20).build());

        // Cancel button
        this.addDrawableChild(ButtonWidget.builder(
                Text.translatable("config.ji_afkcinematic.cancel"),
                button -> this.close()
        ).dimensions(centerX + 55, bottomY, 100, 20).build());
    }

    private Text getDamageActionText() {
        return Text.translatable("config.ji_afkcinematic.damage_action")
                .append(": ")
                .append(Text.translatable("config.ji_afkcinematic.damage_action." + editConfig.damageAction.name().toLowerCase()));
    }

    
    private Text getActiveDisabledText(boolean value) {
        if (value) return Text.literal("§a").append(Text.translatable("config.ji_afkcinematic.active"));
        return Text.literal("§c").append(Text.translatable("config.ji_afkcinematic.disabled"));
    }

    private Text getOnOffText(boolean value) {
        if (value) return Text.literal("\u00A7a").append(Text.translatable("config.ji_afkcinematic.on"));
        return Text.literal("\u00A7c").append(Text.translatable("config.ji_afkcinematic.off"));
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        super.renderBackground(context, mouseX, mouseY, delta);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        if (this.reportButton != null) {
            long time = System.currentTimeMillis() / 800;
            int phase = (int) (time % 3);
            if (phase == 0) this.reportButton.setMessage(Text.literal("\u00A7e\u26A0"));
            else if (phase == 1) this.reportButton.setMessage(Text.literal("\u00A7b\u2666"));
            else this.reportButton.setMessage(Text.literal("\u00A7a\u2709"));
        }

        // Render big logo at top center
        // Deleted drawTexture to fix render pipeline issues in 1.21.11

        // Gold Title
        context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("\u00A76\u00A7lJi AFK Cinematic"), this.width / 2, 55, 0xFFFFFFFF);

        // jiory_ Subtitle
        context.drawCenteredTextWithShadow(this.textRenderer,
                Text.literal("\u00A75By jiory_"),
                this.width / 2, 65, 0xFFFFFFFF);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (waitingForKey) {
            
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                editConfig.configKeyCode = -1;
                waitingForKey = false;
                keyBindButton.setMessage(Text.translatable("config.ji_afkcinematic.keybind", getKeyName(editConfig.configKeyCode)));
                return true;
            }
            if (isKeyReservedOrConflicting(keyCode)) {
                waitingForKey = false;
                keyBindButton.setMessage(Text.translatable("config.ji_afkcinematic.keybind_conflict", getKeyName(keyCode)));
                return true;
            }
            editConfig.configKeyCode = keyCode;
            waitingForKey = false;
            keyBindButton.setMessage(Text.translatable("config.ji_afkcinematic.keybind", getKeyName(keyCode)));
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void close() {
        MinecraftClient.getInstance().setScreen(this.parent);
    }

    private static String getKeyName(int keyCode) {
        if (keyCode == -1) return "NONE";
        String name = GLFW.glfwGetKeyName(keyCode, 0);
        if (name != null) {
            return name.toUpperCase();
        }
        return switch (keyCode) {
            case GLFW.GLFW_KEY_F1 -> "F1";
            case GLFW.GLFW_KEY_F2 -> "F2";
            case GLFW.GLFW_KEY_F3 -> "F3";
            case GLFW.GLFW_KEY_F4 -> "F4";
            case GLFW.GLFW_KEY_F5 -> "F5";
            case GLFW.GLFW_KEY_F6 -> "F6";
            case GLFW.GLFW_KEY_F7 -> "F7";
            case GLFW.GLFW_KEY_F8 -> "F8";
            case GLFW.GLFW_KEY_F9 -> "F9";
            case GLFW.GLFW_KEY_F10 -> "F10";
            case GLFW.GLFW_KEY_F11 -> "F11";
            case GLFW.GLFW_KEY_F12 -> "F12";
            case GLFW.GLFW_KEY_LEFT_SHIFT -> "L-Shift";
            case GLFW.GLFW_KEY_RIGHT_SHIFT -> "R-Shift";
            case GLFW.GLFW_KEY_LEFT_CONTROL -> "L-Ctrl";
            case GLFW.GLFW_KEY_RIGHT_CONTROL -> "R-Ctrl";
            case GLFW.GLFW_KEY_LEFT_ALT -> "L-Alt";
            case GLFW.GLFW_KEY_RIGHT_ALT -> "R-Alt";
            case GLFW.GLFW_KEY_TAB -> "Tab";
            case GLFW.GLFW_KEY_SPACE -> "Space";
            case GLFW.GLFW_KEY_ENTER -> "Enter";
            case GLFW.GLFW_KEY_BACKSPACE -> "Backspace";
            case GLFW.GLFW_KEY_INSERT -> "Insert";
            case GLFW.GLFW_KEY_DELETE -> "Delete";
            case GLFW.GLFW_KEY_HOME -> "Home";
            case GLFW.GLFW_KEY_END -> "End";
            case GLFW.GLFW_KEY_PAGE_UP -> "Page Up";
            case GLFW.GLFW_KEY_PAGE_DOWN -> "Page Down";
            default -> "Key " + keyCode;
        };
    }

    private boolean isKeyReservedOrConflicting(int keyCode) {
        if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE || keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_F3 || keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_F11) {
            return true;
        }
        net.minecraft.client.MinecraftClient client = net.minecraft.client.MinecraftClient.getInstance();
        if (client != null && client.options != null) {
            for (net.minecraft.client.option.KeyBinding binding : client.options.allKeys) {
                if (binding == com.ji.afkcinematic.JiAFKCinematic.configKeyBinding) continue;
                try {
                    net.minecraft.client.util.InputUtil.Key boundKey = net.minecraft.client.util.InputUtil.fromTranslationKey(binding.getBoundKeyTranslationKey());
                    if (boundKey.getCode() == keyCode) return true;
                } catch (Exception e) {}
            }
        }
        return false;
    }
}
