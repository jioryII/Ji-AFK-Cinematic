package com.ji.afkcinematic.config;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

public class ConfigScreen extends Screen {
    private final Screen parent;
    private ModConfig editConfig;
    private enum RebindState { IDLE, MENU_WAITING_FIRST, MENU_WAITING_SECOND, TOGGLE_WAITING_FIRST, TOGGLE_WAITING_SECOND }
    private RebindState rebindState = RebindState.IDLE;
    private int backupMenu1, backupMenu2, backupToggle1, backupToggle2;
    private long rebindStartedMs = 0L;
    private Button menuKeyButton;
    private Button toggleKeyButton;

    private Button reportButton;
    private Button modEnabledButton;

    public ConfigScreen(Screen parent) {
        super(Component.translatable("config.ji_afkcinematic.title"));
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
        this.editConfig.menuKey1 = current.menuKey1;
        this.editConfig.menuKey2 = current.menuKey2;
        this.editConfig.toggleKey1 = current.toggleKey1;
        this.editConfig.toggleKey2 = current.toggleKey2;
        this.editConfig.cinematicMusicVolume = current.cinematicMusicVolume;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int yLeft = 85;
        int yRight = 85;
        int widgetWidth = 135;
        int entryHeight = 26;

        int col1X = centerX - 140;
        int col2X = centerX + 5;

        this.addRenderableWidget(new AbstractSliderButton(
                col1X, yLeft, widgetWidth, 20,
                Component.translatable("config.ji_afkcinematic.music_volume", (int)(editConfig.cinematicMusicVolume * 100)),
                editConfig.cinematicMusicVolume
        ) {
            @Override
            protected void updateMessage() {
                int val = (int) (this.value * 100);
                this.setMessage(Component.translatable("config.ji_afkcinematic.music_volume", val));
            }
            @Override
            protected void applyValue() {
                editConfig.cinematicMusicVolume = (float) this.value;
            }
            { setTooltip(Tooltip.create(Component.translatable("config.ji_afkcinematic.tooltip.music_volume"))); }
        });
        yLeft += entryHeight;

        this.addRenderableWidget(new AbstractSliderButton(
                col1X, yLeft, widgetWidth, 20,
                Component.translatable("config.ji_afkcinematic.shot_duration", editConfig.shotDurationSeconds),
                (editConfig.shotDurationSeconds - 5.0) / 55.0
        ) {
            @Override
            protected void updateMessage() {
                int val = 5 + (int) (this.value * 55);
                this.setMessage(Component.translatable("config.ji_afkcinematic.shot_duration", val));
            }
            @Override
            protected void applyValue() {
                editConfig.shotDurationSeconds = 5 + (int) (this.value * 55);
            }
            { setTooltip(Tooltip.create(Component.translatable("config.ji_afkcinematic.tooltip.shot_duration"))); }
        });
        yLeft += entryHeight;

        this.addRenderableWidget(new AbstractSliderButton(
                col1X, yLeft, widgetWidth, 20,
                Component.translatable("config.ji_afkcinematic.afk_threshold", editConfig.afkThresholdSeconds),
                (editConfig.afkThresholdSeconds - 10.0) / 590.0
        ) {
            @Override
            protected void updateMessage() {
                int val = 10 + (int) (this.value * 590);
                this.setMessage(Component.translatable("config.ji_afkcinematic.afk_threshold", val));
            }
            @Override
            protected void applyValue() {
                editConfig.afkThresholdSeconds = 10 + (int) (this.value * 590);
            }
            { setTooltip(Tooltip.create(Component.translatable("config.ji_afkcinematic.tooltip.afk_threshold"))); }
        });
        yLeft += entryHeight;

        this.addRenderableWidget(new AbstractSliderButton(
                col1X, yLeft, widgetWidth, 20,
                Component.translatable("config.ji_afkcinematic.max_cycles", editConfig.maxCycles),
                (editConfig.maxCycles - 1.0) / 19.0
        ) {
            @Override
            protected void updateMessage() {
                int val = 1 + (int) (this.value * 19);
                this.setMessage(Component.translatable("config.ji_afkcinematic.max_cycles", val));
            }
            @Override
            protected void applyValue() {
                editConfig.maxCycles = 1 + (int) (this.value * 19);
            }
            { setTooltip(Tooltip.create(Component.translatable("config.ji_afkcinematic.tooltip.max_cycles"))); }
        });
        yLeft += entryHeight;

        this.addRenderableWidget(new AbstractSliderButton(
                col1X, yLeft, widgetWidth, 20,
                Component.translatable("config.ji_afkcinematic.camera_speed", String.format("%.1f", editConfig.cameraSpeed)),
                (editConfig.cameraSpeed - 0.1) / 2.9
        ) {
            @Override
            protected void updateMessage() {
                float val = 0.1f + (float) (this.value * 2.9);
                this.setMessage(Component.translatable("config.ji_afkcinematic.camera_speed", String.format("%.1f", val)));
            }
            @Override
            protected void applyValue() {
                editConfig.cameraSpeed = 0.1f + (float) (this.value * 2.9);
            }
            { setTooltip(Tooltip.create(Component.translatable("config.ji_afkcinematic.tooltip.camera_speed"))); }
        });
        yLeft += entryHeight;

        this.addRenderableWidget(Button.builder(
                getDamageActionText(),
                button -> {
                    int nextOrdinal = (editConfig.damageAction.ordinal() + 1) % DamageAction.values().length;
                    editConfig.damageAction = DamageAction.values()[nextOrdinal];
                    button.setMessage(getDamageActionText());
                }
        ).tooltip(Tooltip.create(Component.translatable("config.ji_afkcinematic.tooltip.damage_action"))).bounds(col2X, yRight, widgetWidth, 20).build());
        yRight += entryHeight;

        this.addRenderableWidget(Button.builder(
                Component.translatable("config.ji_afkcinematic.letterbox").append(": ").append(getOnOffText(editConfig.enableLetterbox)),
                button -> {
                    editConfig.enableLetterbox = !editConfig.enableLetterbox;
                    button.setMessage(Component.translatable("config.ji_afkcinematic.letterbox").append(": ").append(getOnOffText(editConfig.enableLetterbox)));
                }
        ).tooltip(Tooltip.create(Component.translatable("config.ji_afkcinematic.tooltip.letterbox"))).bounds(col2X, yRight, widgetWidth, 20).build());
        yRight += entryHeight;

        this.addRenderableWidget(Button.builder(
                Component.translatable("config.ji_afkcinematic.music").append(": ").append(getOnOffText(editConfig.enableMusic)),
                button -> {
                    editConfig.enableMusic = !editConfig.enableMusic;
                    button.setMessage(Component.translatable("config.ji_afkcinematic.music").append(": ").append(getOnOffText(editConfig.enableMusic)));
                }
        ).tooltip(Tooltip.create(Component.translatable("config.ji_afkcinematic.tooltip.music"))).bounds(col2X, yRight, widgetWidth, 20).build());
        yRight += entryHeight;

        this.addRenderableWidget(Button.builder(
                Component.translatable("config.ji_afkcinematic.extended_music").append(": ").append(getOnOffText(editConfig.extendedMusic)),
                button -> {
                    editConfig.extendedMusic = !editConfig.extendedMusic;
                    button.setMessage(Component.translatable("config.ji_afkcinematic.extended_music").append(": ").append(getOnOffText(editConfig.extendedMusic)));
                }
        ).tooltip(Tooltip.create(Component.translatable("config.ji_afkcinematic.tooltip.extended_music"))).bounds(col2X, yRight, widgetWidth, 20).build());
        yRight += entryHeight;

        int centerStartY = Math.max(yLeft, yRight) + 5;

        menuKeyButton = Button.builder(
            getMenuKeysText(),
            button -> startMenuRebind()
        ).tooltip(Tooltip.create(Component.translatable("config.ji_afkcinematic.tooltip.menu_keys")))
         .bounds(centerX - widgetWidth / 2, centerStartY, widgetWidth, 20).build();
        this.addRenderableWidget(menuKeyButton);
        centerStartY += entryHeight;

        toggleKeyButton = Button.builder(
            getToggleKeysText(),
            button -> startToggleRebind()
        ).tooltip(Tooltip.create(Component.translatable("config.ji_afkcinematic.tooltip.toggle_keys")))
         .bounds(centerX - widgetWidth / 2, centerStartY, widgetWidth, 20).build();
        this.addRenderableWidget(toggleKeyButton);
        centerStartY += entryHeight;

        modEnabledButton = Button.builder(
                Component.translatable("config.ji_afkcinematic.enabled").append(": ").append(getActiveDisabledText(editConfig.modEnabled)),
                button -> {
                    editConfig.modEnabled = !editConfig.modEnabled;
                    button.setMessage(Component.translatable("config.ji_afkcinematic.enabled").append(": ").append(getActiveDisabledText(editConfig.modEnabled)));
                }
        ).tooltip(Tooltip.create(Component.translatable("config.ji_afkcinematic.tooltip.enabled"))).bounds(centerX - widgetWidth / 2, centerStartY, widgetWidth, 20).build();
        this.addRenderableWidget(modEnabledButton);

        this.reportButton = Button.builder(
                Component.literal("\u00A7e\u26A0"),
                ConfirmLinkScreen.confirmLink(this, "https://discord.gg/sE27D5SNaq")
        ).tooltip(Tooltip.create(Component.translatable("config.ji_afkcinematic.tooltip.report"))).bounds(this.width - 35, this.height - 35, 30, 30).build();
        this.addRenderableWidget(this.reportButton);

        int bottomY = this.height - 35;

        this.addRenderableWidget(Button.builder(
                Component.translatable("config.ji_afkcinematic.reset_defaults"),
                button -> {
                    this.editConfig = new ModConfig();
                    this.rebuildWidgets();
                }
        ).bounds(centerX - 155, bottomY, 100, 20).build());

        this.addRenderableWidget(Button.builder(
                Component.translatable("config.ji_afkcinematic.save"),
                button -> {
                    editConfig.recalculate();
                    ConfigManager.setConfig(editConfig);
                    this.onClose();
                }
        ).bounds(centerX - 50, bottomY, 100, 20).build());

        this.addRenderableWidget(Button.builder(
                Component.translatable("config.ji_afkcinematic.cancel"),
                button -> this.onClose()
        ).bounds(centerX + 55, bottomY, 100, 20).build());
    }

    private Component getDamageActionText() {
        return Component.translatable("config.ji_afkcinematic.damage_action")
                .append(": ")
                .append(Component.translatable("config.ji_afkcinematic.damage_action." + editConfig.damageAction.name().toLowerCase()));
    }

    private Component getMenuKeysText() {
        String keys = formatKeys(editConfig.menuKey1, editConfig.menuKey2);
        return Component.empty()
                .append(Component.translatable("config.ji_afkcinematic.menu_keys.label").withStyle(ChatFormatting.WHITE))
                .append(Component.literal(keys).withStyle(ChatFormatting.YELLOW));
    }

    private Component getToggleKeysText() {
        String keys = formatKeys(editConfig.toggleKey1, editConfig.toggleKey2);
        return Component.empty()
                .append(Component.translatable("config.ji_afkcinematic.toggle_keys.label").withStyle(ChatFormatting.WHITE))
                .append(Component.literal(keys).withStyle(ChatFormatting.YELLOW));
    }

    private Component getActiveDisabledText(boolean value) {
        if (value) return Component.literal("\u00A7a").append(Component.translatable("config.ji_afkcinematic.active"));
        return Component.literal("\u00A7c").append(Component.translatable("config.ji_afkcinematic.disabled"));
    }

    private Component getOnOffText(boolean value) {
        if (value) return Component.literal("\u00A7a").append(Component.translatable("config.ji_afkcinematic.on"));
        return Component.literal("\u00A7c").append(Component.translatable("config.ji_afkcinematic.off"));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        super.extractRenderState(context, mouseX, mouseY, delta);

        if (this.reportButton != null) {
            long time = System.currentTimeMillis() / 800;
            int phase = (int) (time % 3);
            if (phase == 0) this.reportButton.setMessage(Component.literal("\u00A7e\u26A0"));
            else if (phase == 1) this.reportButton.setMessage(Component.literal("\u00A7b\u2666"));
            else this.reportButton.setMessage(Component.literal("\u00A7a\u2709"));
        }

        context.centeredText(this.font, Component.literal("\u00A76\u00A7lJi AFK Cinematic"), this.width / 2, 55, 0xFFFFFFFF);
        context.centeredText(this.font, Component.literal("\u00A75By jiory_"), this.width / 2, 65, 0xFFFFFFFF);
        // Sin mensaje redundante: el feedback del rebind aparece solo en el boton correspondiente.
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int keyCode = event.key();
        if (rebindState == RebindState.IDLE && keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) {
            this.onClose();
            return true;
        }
        if (rebindState != RebindState.IDLE) {
            // ESC durante rebind:
            //   - Antes de pulsar la primera tecla -> ambas teclas = -1 (NONE)
            //   - Despues de pulsar la primera -> cancelar y restaurar como estaba
            if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) {
                if (rebindState == RebindState.MENU_WAITING_FIRST || rebindState == RebindState.TOGGLE_WAITING_FIRST) {
                    if (rebindState == RebindState.MENU_WAITING_FIRST) {
                        editConfig.menuKey1 = -1;
                        editConfig.menuKey2 = -1;
                    } else {
                        editConfig.toggleKey1 = -1;
                        editConfig.toggleKey2 = -1;
                    }
                    rebindState = RebindState.IDLE;
                    com.ji.afkcinematic.input.KeySequenceTracker.resetRebind();
                    refreshKeyButtonLabels();
                } else {
                    cancelRebind();
                }
                return true;
            }
            // Timeout: si estamos esperando la 1a o 2a tecla y pasaron >1.5s -> restaurar
            if (rebindState == RebindState.MENU_WAITING_FIRST || rebindState == RebindState.TOGGLE_WAITING_FIRST) {
                if (System.currentTimeMillis() - rebindStartedMs > com.ji.afkcinematic.input.KeySequenceTracker.SEQUENCE_TIMEOUT_MS) {
                    cancelRebind();
                    return true;
                }
            } else {
                if (com.ji.afkcinematic.input.KeySequenceTracker.getRebindRemainingMs() <= 0L) {
                    cancelRebind();
                    return true;
                }
            }

            int[] out = new int[2];
            int result = com.ji.afkcinematic.input.KeySequenceTracker.processRebindKey(keyCode, out);
            if (result == -1) { cancelRebind(); return true; }

            if (rebindState == RebindState.MENU_WAITING_FIRST || rebindState == RebindState.MENU_WAITING_SECOND) {
                if (result == 1) {
                    rebindState = RebindState.MENU_WAITING_SECOND;
                    menuKeyButton.setMessage(Component.translatable("config.ji_afkcinematic.key_waiting_second"));
                                    } else if (result == 2) {
                    editConfig.menuKey1 = out[0];
                    editConfig.menuKey2 = out[1];
                    rebindState = RebindState.IDLE; com.ji.afkcinematic.input.KeySequenceTracker.resetRebind(); refreshKeyButtonLabels();

                }
            } else {
                if (result == 1) {
                    rebindState = RebindState.TOGGLE_WAITING_SECOND;
                    toggleKeyButton.setMessage(Component.translatable("config.ji_afkcinematic.key_waiting_second"));
                                    } else if (result == 2) {
                    editConfig.toggleKey1 = out[0];
                    editConfig.toggleKey2 = out[1];
                    rebindState = RebindState.IDLE; com.ji.afkcinematic.input.KeySequenceTracker.resetRebind(); refreshKeyButtonLabels();

                }
            }
            return true;
        }

        return super.keyPressed(event);
    }

    @Override
    public void onClose() {
        com.ji.afkcinematic.ScreenHelper.setScreen(Minecraft.getInstance(), this.parent);
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
            case GLFW.GLFW_KEY_LEFT_SHIFT -> "Shift";
            case GLFW.GLFW_KEY_RIGHT_SHIFT -> "Shift";
            case GLFW.GLFW_KEY_LEFT_CONTROL -> "Ctrl";
            case GLFW.GLFW_KEY_RIGHT_CONTROL -> "Ctrl";
            case GLFW.GLFW_KEY_LEFT_ALT -> "Alt";
            case GLFW.GLFW_KEY_RIGHT_ALT -> "Alt";
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

    /**
     * Formatea la visualizacion de las dos teclas: si ambas son NONE muestra "NONE",
     * si solo una es NONE muestra "A + NONE" y si ambas son teclas reales muestra "A + B".
     */
    private static String formatKeys(int k1, int k2) {
        String n1 = getKeyName(k1);
        String n2 = getKeyName(k2);
        boolean isNone1 = (k1 == -1);
        boolean isNone2 = (k2 == -1);
        if (isNone1 && isNone2) return "NONE";
        if (isNone1) return n2 + " + NONE";
        if (isNone2) return n1 + " + NONE";
        return n1 + " + " + n2;
    }
    private void startMenuRebind() {
        backupMenu1 = editConfig.menuKey1; backupMenu2 = editConfig.menuKey2;
        rebindState = RebindState.MENU_WAITING_FIRST; com.ji.afkcinematic.input.KeySequenceTracker.startRebind();
        rebindStartedMs = System.currentTimeMillis();
        menuKeyButton.setMessage(Component.translatable("config.ji_afkcinematic.key_waiting_first"));

    }
    private void startToggleRebind() {
        backupToggle1 = editConfig.toggleKey1; backupToggle2 = editConfig.toggleKey2;
        rebindState = RebindState.TOGGLE_WAITING_FIRST; com.ji.afkcinematic.input.KeySequenceTracker.startRebind();
        rebindStartedMs = System.currentTimeMillis();
        toggleKeyButton.setMessage(Component.translatable("config.ji_afkcinematic.key_waiting_first"));

    }
    private void cancelRebind() {
        if (rebindState == RebindState.MENU_WAITING_FIRST || rebindState == RebindState.MENU_WAITING_SECOND) {
            editConfig.menuKey1 = backupMenu1; editConfig.menuKey2 = backupMenu2;
        } else {
            editConfig.toggleKey1 = backupToggle1; editConfig.toggleKey2 = backupToggle2;
        }
        rebindState = RebindState.IDLE; com.ji.afkcinematic.input.KeySequenceTracker.resetRebind();
        refreshKeyButtonLabels();     }
    private void refreshKeyButtonLabels() {
        menuKeyButton.setMessage(Component.translatable("config.ji_afkcinematic.menu_keys", getKeyName(editConfig.menuKey1), getKeyName(editConfig.menuKey2)));
        toggleKeyButton.setMessage(Component.translatable("config.ji_afkcinematic.toggle_keys", getKeyName(editConfig.toggleKey1), getKeyName(editConfig.toggleKey2)));
    }
}