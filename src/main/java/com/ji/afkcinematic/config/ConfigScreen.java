package com.ji.afkcinematic.config;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

public class ConfigScreen extends Screen {
    private final Screen parent;
    private ModConfig editConfig;
    private boolean waitingForKey = false;
    private ButtonWidget keyBindButton;
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
        this.editConfig.showPlayerName = current.showPlayerName;
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

        int col1X = centerX - 165;
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
        });
        this.addDrawableChild(ButtonWidget.builder(Text.literal("i"), b -> {})
                .tooltip(Tooltip.of(Text.translatable("config.ji_afkcinematic.tooltip.shot_duration")))
                .dimensions(col1X + widgetWidth + 5, yLeft, infoWidth, 20).build());
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
        });
        this.addDrawableChild(ButtonWidget.builder(Text.literal("i"), b -> {})
                .tooltip(Tooltip.of(Text.translatable("config.ji_afkcinematic.tooltip.afk_threshold")))
                .dimensions(col1X + widgetWidth + 5, yLeft, infoWidth, 20).build());
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
        });
        this.addDrawableChild(ButtonWidget.builder(Text.literal("i"), b -> {})
                .tooltip(Tooltip.of(Text.translatable("config.ji_afkcinematic.tooltip.max_cycles")))
                .dimensions(col1X + widgetWidth + 5, yLeft, infoWidth, 20).build());
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
        });
        this.addDrawableChild(ButtonWidget.builder(Text.literal("i"), b -> {})
                .tooltip(Tooltip.of(Text.translatable("config.ji_afkcinematic.tooltip.camera_speed")))
                .dimensions(col1X + widgetWidth + 5, yLeft, infoWidth, 20).build());
        yLeft += entryHeight;

        // Damage Action Button
        this.addDrawableChild(ButtonWidget.builder(
                getDamageActionText(),
                button -> {
                    int nextOrdinal = (editConfig.damageAction.ordinal() + 1) % DamageAction.values().length;
                    editConfig.damageAction = DamageAction.values()[nextOrdinal];
                    button.setMessage(getDamageActionText());
                }
        ).dimensions(col1X, yLeft, widgetWidth, 20).build());
        this.addDrawableChild(ButtonWidget.builder(Text.literal("i"), b -> {})
                .tooltip(Tooltip.of(Text.translatable("config.ji_afkcinematic.tooltip.damage_action")))
                .dimensions(col1X + widgetWidth + 5, yLeft, infoWidth, 20).build());

        // --- Right Column ---

        // Show Player Name toggle
        this.addDrawableChild(ButtonWidget.builder(
                Text.translatable("config.ji_afkcinematic.show_name").append(": ").append(getOnOffText(editConfig.showPlayerName)),
                button -> {
                    editConfig.showPlayerName = !editConfig.showPlayerName;
                    button.setMessage(Text.translatable("config.ji_afkcinematic.show_name").append(": ").append(getOnOffText(editConfig.showPlayerName)));
                }
        ).dimensions(col2X, yRight, widgetWidth, 20).build());
        this.addDrawableChild(ButtonWidget.builder(Text.literal("i"), b -> {})
                .tooltip(Tooltip.of(Text.translatable("config.ji_afkcinematic.tooltip.show_name")))
                .dimensions(col2X + widgetWidth + 5, yRight, infoWidth, 20).build());
        yRight += entryHeight;

        // Letterbox toggle
        this.addDrawableChild(ButtonWidget.builder(
                Text.translatable("config.ji_afkcinematic.letterbox").append(": ").append(getOnOffText(editConfig.enableLetterbox)),
                button -> {
                    editConfig.enableLetterbox = !editConfig.enableLetterbox;
                    button.setMessage(Text.translatable("config.ji_afkcinematic.letterbox").append(": ").append(getOnOffText(editConfig.enableLetterbox)));
                }
        ).dimensions(col2X, yRight, widgetWidth, 20).build());
        this.addDrawableChild(ButtonWidget.builder(Text.literal("i"), b -> {})
                .tooltip(Tooltip.of(Text.translatable("config.ji_afkcinematic.tooltip.letterbox")))
                .dimensions(col2X + widgetWidth + 5, yRight, infoWidth, 20).build());
        yRight += entryHeight;

        // Music toggle
        this.addDrawableChild(ButtonWidget.builder(
                Text.translatable("config.ji_afkcinematic.music").append(": ").append(getOnOffText(editConfig.enableMusic)),
                button -> {
                    editConfig.enableMusic = !editConfig.enableMusic;
                    button.setMessage(Text.translatable("config.ji_afkcinematic.music").append(": ").append(getOnOffText(editConfig.enableMusic)));
                }
        ).dimensions(col2X, yRight, widgetWidth, 20).build());
        this.addDrawableChild(ButtonWidget.builder(Text.literal("i"), b -> {})
                .tooltip(Tooltip.of(Text.translatable("config.ji_afkcinematic.tooltip.music")))
                .dimensions(col2X + widgetWidth + 5, yRight, infoWidth, 20).build());
        yRight += entryHeight;

        // Config Keybind button
        keyBindButton = ButtonWidget.builder(
                Text.translatable("config.ji_afkcinematic.keybind", getKeyName(editConfig.configKeyCode)),
                button -> {
                    waitingForKey = true;
                    button.setMessage(Text.translatable("config.ji_afkcinematic.keybind_waiting"));
                }
        ).dimensions(col2X, yRight, widgetWidth, 20).build();
        this.addDrawableChild(keyBindButton);
        this.addDrawableChild(ButtonWidget.builder(Text.literal("i"), b -> {})
                .tooltip(Tooltip.of(Text.translatable("config.ji_afkcinematic.tooltip.keybind")))
                .dimensions(col2X + widgetWidth + 5, yRight, infoWidth, 20).build());

        // --- Bottom Centered Buttons ---
        
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

    private Text getOnOffText(boolean value) {
        if (value) return Text.literal("\u00A7a").append(Text.translatable("config.ji_afkcinematic.on"));
        return Text.literal("\u00A7c").append(Text.translatable("config.ji_afkcinematic.off"));
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderInGameBackground(context);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        // Render big logo at top center
        // Deleted drawTexture to fix render pipeline issues in 1.21.11

        // Gold Title
        context.drawCenteredTextWithShadow(this.textRenderer,
                Text.literal("\u00A76\u00A7lJi AFK Cinematic"),
                this.width / 2, 55, 0xFFFFFF);

        // Purple Subtitle (Author info)
        context.drawCenteredTextWithShadow(this.textRenderer,
                Text.literal("\u00A75Por Jiory_"),
                this.width / 2, 68, 0xFFFFFF);
    }

    @Override
    public boolean keyPressed(KeyInput keyInput) {
        if (waitingForKey) {
            int keyCode = keyInput.key();
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                waitingForKey = false;
                keyBindButton.setMessage(Text.translatable("config.ji_afkcinematic.keybind", getKeyName(editConfig.configKeyCode)));
                return true;
            }
            editConfig.configKeyCode = keyCode;
            waitingForKey = false;
            keyBindButton.setMessage(Text.translatable("config.ji_afkcinematic.keybind", getKeyName(keyCode)));
            return true;
        }
        return super.keyPressed(keyInput);
    }

    @Override
    public void close() {
        MinecraftClient.getInstance().setScreen(this.parent);
    }

    private static String getKeyName(int keyCode) {
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
}
