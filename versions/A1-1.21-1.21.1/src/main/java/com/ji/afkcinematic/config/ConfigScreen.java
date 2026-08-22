package com.ji.afkcinematic.config;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ConfirmLinkScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

public class ConfigScreen extends Screen {
    private final Screen parent;
    private ModConfig editConfig;
    private enum RebindState { IDLE, MENU_WAITING_FIRST, MENU_WAITING_SECOND, TOGGLE_WAITING_FIRST, TOGGLE_WAITING_SECOND }
    private RebindState rebindState = RebindState.IDLE;
    private int backupMenu1, backupMenu2, backupToggle1, backupToggle2;
    private long rebindStartedMs = 0L;
    private ButtonWidget menuKeyButton;
    private ButtonWidget toggleKeyButton;
    private ButtonWidget reportButton;
    private ButtonWidget modEnabledButton;

    // Estado para restaurar el label tras UNSUPPORTED_KEY_REJECTED.
    // Cuando processRebindKey devuelve 3, mostramos un mensaje rojo temporal
    // (~1.5s) y luego restauramos el label original. Usamos un counter en
    // tick() en vez de threads (todo corre en el client thread).
    private ButtonWidget pendingLabelTarget;
    private Text pendingLabelOriginal;
    private int pendingLabelTicksRemaining;

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
        this.editConfig.characterShotPercentage = current.characterShotPercentage;
        this.editConfig.persistentMode = current.persistentMode;
        this.editConfig.cameraRotationEnabled = current.cameraRotationEnabled;
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

        this.addDrawableChild(new SliderWidget(
                col1X, yLeft, widgetWidth, 20,
                Text.translatable("config.ji_afkcinematic.music_volume", (int)(editConfig.cinematicMusicVolume * 100)),
                editConfig.cinematicMusicVolume
        ) {
            @Override
            protected void updateMessage() {
                int val = (int) (this.value * 100);
                this.setMessage(Text.translatable("config.ji_afkcinematic.music_volume", val));
            }
            @Override
            protected void applyValue() {
                editConfig.cinematicMusicVolume = (float) this.value;
            }
            { setTooltip(Tooltip.of(Text.translatable("config.ji_afkcinematic.tooltip.music_volume"))); }
        });
        yLeft += entryHeight;

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

        this.addDrawableChild(new SliderWidget(
                col1X, yLeft, widgetWidth, 20,
                Text.translatable("config.ji_afkcinematic.shot_mix",
                        editConfig.characterShotPercentage, 100 - editConfig.characterShotPercentage),
                editConfig.characterShotPercentage / 100.0
        ) {
            private int snappedValue() { return (int) Math.round(this.value * 10.0) * 10; }
            @Override
            protected void updateMessage() {
                int val = snappedValue();
                this.setMessage(Text.translatable("config.ji_afkcinematic.shot_mix", val, 100 - val));
            }
            @Override
            protected void applyValue() {
                int val = snappedValue();
                this.value = val / 100.0;
                editConfig.characterShotPercentage = val;
                updateMessage();
            }
            { setTooltip(Tooltip.of(Text.translatable("config.ji_afkcinematic.tooltip.shot_mix"))); }
        });
        yLeft += entryHeight;

        this.addDrawableChild(ButtonWidget.builder(
                getDamageActionText(),
                button -> {
                    int nextOrdinal = (editConfig.damageAction.ordinal() + 1) % DamageAction.values().length;
                    editConfig.damageAction = DamageAction.values()[nextOrdinal];
                    button.setMessage(getDamageActionText());
                }
        ).tooltip(Tooltip.of(Text.translatable("config.ji_afkcinematic.tooltip.damage_action"))).dimensions(col2X, yRight, widgetWidth, 20).build());
        yRight += entryHeight;

        this.addDrawableChild(ButtonWidget.builder(
                getPersistentModeText(),
                button -> {
                    editConfig.persistentMode = editConfig.persistentMode.next();
                    button.setMessage(getPersistentModeText());
                }
        ).tooltip(Tooltip.of(Text.translatable("config.ji_afkcinematic.tooltip.persistent_cinematics")
                .append("\n").append(Text.literal("[BETA]").formatted(Formatting.YELLOW))))
         .dimensions(col2X, yRight, widgetWidth, 20).build());
        yRight += entryHeight;

        this.addDrawableChild(ButtonWidget.builder(
                Text.translatable("config.ji_afkcinematic.camera_rotation")
                        .append(": ").append(getOnOffText(editConfig.cameraRotationEnabled)),
                button -> {
                    editConfig.cameraRotationEnabled = !editConfig.cameraRotationEnabled;
                    button.setMessage(Text.translatable("config.ji_afkcinematic.camera_rotation")
                            .append(": ").append(getOnOffText(editConfig.cameraRotationEnabled)));
                }
        ).tooltip(Tooltip.of(Text.translatable("config.ji_afkcinematic.tooltip.camera_rotation")))
         .dimensions(col2X, yRight, widgetWidth, 20).build());
        yRight += entryHeight;

        this.addDrawableChild(ButtonWidget.builder(
                Text.translatable("config.ji_afkcinematic.letterbox").append(": ").append(getOnOffText(editConfig.enableLetterbox)),
                button -> {
                    editConfig.enableLetterbox = !editConfig.enableLetterbox;
                    button.setMessage(Text.translatable("config.ji_afkcinematic.letterbox").append(": ").append(getOnOffText(editConfig.enableLetterbox)));
                }
        ).tooltip(Tooltip.of(Text.translatable("config.ji_afkcinematic.tooltip.letterbox"))).dimensions(col2X, yRight, widgetWidth, 20).build());
        yRight += entryHeight;

        this.addDrawableChild(ButtonWidget.builder(
                Text.translatable("config.ji_afkcinematic.music").append(": ").append(getOnOffText(editConfig.enableMusic)),
                button -> {
                    editConfig.enableMusic = !editConfig.enableMusic;
                    button.setMessage(Text.translatable("config.ji_afkcinematic.music").append(": ").append(getOnOffText(editConfig.enableMusic)));
                }
        ).tooltip(Tooltip.of(Text.translatable("config.ji_afkcinematic.tooltip.music"))).dimensions(col2X, yRight, widgetWidth, 20).build());
        yRight += entryHeight;

        this.addDrawableChild(ButtonWidget.builder(
                Text.translatable("config.ji_afkcinematic.extended_music").append(": ").append(getOnOffText(editConfig.extendedMusic)),
                button -> {
                    editConfig.extendedMusic = !editConfig.extendedMusic;
                    button.setMessage(Text.translatable("config.ji_afkcinematic.extended_music").append(": ").append(getOnOffText(editConfig.extendedMusic)));
                }
        ).tooltip(Tooltip.of(Text.translatable("config.ji_afkcinematic.tooltip.extended_music"))).dimensions(col2X, yRight, widgetWidth, 20).build());
        yRight += entryHeight;

        int centerStartY = Math.max(yLeft, yRight) + 5;

        // Menu shortcut. Clicking it starts the normal two-key rebind flow.
        menuKeyButton = ButtonWidget.builder(
            getMenuKeysText(),
            button -> startMenuRebind()
        ).tooltip(Tooltip.of(Text.translatable("config.ji_afkcinematic.tooltip.menu_keys")))
         .dimensions(centerX - widgetWidth / 2, centerStartY, widgetWidth, 20).build();
        this.addDrawableChild(menuKeyButton);

        centerStartY += entryHeight;

        // Quick-toggle shortcut.
        toggleKeyButton = ButtonWidget.builder(
            getToggleKeysText(),
            button -> startToggleRebind()
        ).tooltip(Tooltip.of(Text.translatable("config.ji_afkcinematic.tooltip.toggle_keys")))
         .dimensions(centerX - widgetWidth / 2, centerStartY, widgetWidth, 20).build();
        this.addDrawableChild(toggleKeyButton);

        centerStartY += entryHeight;

        modEnabledButton = ButtonWidget.builder(
                Text.translatable("config.ji_afkcinematic.enabled").append(": ").append(getActiveDisabledText(editConfig.modEnabled)),
                button -> {
                    editConfig.modEnabled = !editConfig.modEnabled;
                    button.setMessage(Text.translatable("config.ji_afkcinematic.enabled").append(": ").append(getActiveDisabledText(editConfig.modEnabled)));
                }
        ).tooltip(Tooltip.of(Text.translatable("config.ji_afkcinematic.tooltip.enabled"))).dimensions(centerX - widgetWidth / 2, centerStartY, widgetWidth, 20).build();
        this.addDrawableChild(modEnabledButton);

        this.reportButton = ButtonWidget.builder(
                Text.literal("§e⚠"),
                ConfirmLinkScreen.opening(this, "https://discord.gg/sE27D5SNaq")
        ).tooltip(Tooltip.of(Text.translatable("config.ji_afkcinematic.tooltip.report"))).dimensions(this.width - 35, this.height - 35, 30, 30).build();
        this.addDrawableChild(this.reportButton);

        int bottomY = this.height - 35;

        this.addDrawableChild(ButtonWidget.builder(
                Text.translatable("config.ji_afkcinematic.reset_defaults"),
                button -> {
                    this.editConfig = new ModConfig();
                    this.clearAndInit();
                }
        ).dimensions(centerX - 155, bottomY, 100, 20).build());

        this.addDrawableChild(ButtonWidget.builder(
                Text.translatable("config.ji_afkcinematic.save"),
                button -> {
                    editConfig.recalculate();
                    ConfigManager.setConfig(editConfig);
                    this.close();
                }
        ).dimensions(centerX - 50, bottomY, 100, 20).build());

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

    private Text getPersistentModeText() {
        Formatting color = switch (editConfig.persistentMode) {
            case NORMAL -> Formatting.GRAY;
            case INTERACTIVE -> Formatting.YELLOW;
            case PERSISTENT -> Formatting.RED;
        };
        String key = "config.ji_afkcinematic.persistent_mode."
                + editConfig.persistentMode.name().toLowerCase();
        return Text.translatable("config.ji_afkcinematic.persistent_cinematics")
                .append(": ").append(Text.translatable(key).formatted(color));
    }

    private Text getMenuKeysText() {
        if (isMenuDisabled()) {
            return Text.empty()
                    .append(Text.translatable("config.ji_afkcinematic.menu_keys.label").formatted(Formatting.WHITE))
                    .append(Text.translatable("config.ji_afkcinematic.keybind_disabled_label"));
        }
        String keys = formatKeys(editConfig.menuKey1, editConfig.menuKey2);
        return Text.empty()
                .append(Text.translatable("config.ji_afkcinematic.menu_keys.label").formatted(Formatting.WHITE))
                .append(Text.literal(keys).formatted(Formatting.YELLOW));
    }

    private Text getToggleKeysText() {
        if (isToggleDisabled()) {
            return Text.empty()
                    .append(Text.translatable("config.ji_afkcinematic.toggle_keys.label").formatted(Formatting.WHITE))
                    .append(Text.translatable("config.ji_afkcinematic.keybind_disabled_label"));
        }
        String keys = formatKeys(editConfig.toggleKey1, editConfig.toggleKey2);
        return Text.empty()
                .append(Text.translatable("config.ji_afkcinematic.toggle_keys.label").formatted(Formatting.WHITE))
                .append(Text.literal(keys).formatted(Formatting.YELLOW));
    }

    private Text getActiveDisabledText(boolean value) {
        if (value) return Text.literal("§a").append(Text.translatable("config.ji_afkcinematic.active"));
        return Text.literal("§c").append(Text.translatable("config.ji_afkcinematic.disabled"));
    }

    private Text getOnOffText(boolean value) {
        if (value) return Text.literal("§a").append(Text.translatable("config.ji_afkcinematic.on"));
        return Text.literal("§c").append(Text.translatable("config.ji_afkcinematic.off"));
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        if (this.reportButton != null) {
            long time = System.currentTimeMillis() / 800;
            int phase = (int) (time % 3);
            if (phase == 0) this.reportButton.setMessage(Text.literal("§e⚠"));
            else if (phase == 1) this.reportButton.setMessage(Text.literal("§b♦"));
            else this.reportButton.setMessage(Text.literal("§a✉"));
        }

        context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("§6§lJi AFK Cinematic"), this.width / 2, 55, 0xFFFFFFFF);
        context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("§5By jiory_"), this.width / 2, 65, 0xFFFFFFFF);
    }

    /**
     * Hardening v2.2.2: contador en client tick para restaurar el label tras
     * UNSUPPORTED_KEY_REJECTED. Llamado por Screen cada frame.
     */
    @Override
    public void tick() {
        super.tick();
        if (pendingLabelTicksRemaining > 0 && pendingLabelTarget != null) {
            pendingLabelTicksRemaining--;
            if (pendingLabelTicksRemaining == 0 && pendingLabelOriginal != null) {
                pendingLabelTarget.setMessage(pendingLabelOriginal);
                pendingLabelTarget = null;
                pendingLabelOriginal = null;
            }
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
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

            // GLFW_KEY_UNKNOWN y codigos fuera de rango no son atajos persistibles.
            if (result == com.ji.afkcinematic.input.KeySequenceTracker.UNSUPPORTED_KEY_REJECTED) {
                ButtonWidget target = (rebindState == RebindState.MENU_WAITING_FIRST
                                  || rebindState == RebindState.MENU_WAITING_SECOND)
                                  ? menuKeyButton : toggleKeyButton;
                pendingLabelTarget = target;
                pendingLabelOriginal = target.getMessage();
                target.setMessage(Text.literal("Unsupported key")
                    .formatted(Formatting.RED));
                // 30 ticks = 1.5s @ 20 TPS
                pendingLabelTicksRemaining = 30;
                return true;
            }

            if (rebindState == RebindState.MENU_WAITING_FIRST || rebindState == RebindState.MENU_WAITING_SECOND) {
                if (result == 1) {
                    rebindState = RebindState.MENU_WAITING_SECOND;
                    menuKeyButton.setMessage(Text.translatable("config.ji_afkcinematic.key_waiting_second"));
                } else if (result == 2) {
                    editConfig.menuKey1 = out[0];
                    editConfig.menuKey2 = out[1];
                    rebindState = RebindState.IDLE; com.ji.afkcinematic.input.KeySequenceTracker.resetRebind(); refreshKeyButtonLabels();

                }
            } else {
                if (result == 1) {
                    rebindState = RebindState.TOGGLE_WAITING_SECOND;
                    toggleKeyButton.setMessage(Text.translatable("config.ji_afkcinematic.key_waiting_second"));
                } else if (result == 2) {
                    editConfig.toggleKey1 = out[0];
                    editConfig.toggleKey2 = out[1];
                    rebindState = RebindState.IDLE; com.ji.afkcinematic.input.KeySequenceTracker.resetRebind(); refreshKeyButtonLabels();

                }
            }
            return true;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void close() {
        MinecraftClient.getInstance().setScreen(this.parent);
    }

    /** Nombre legible para keycodes GLFW validos. */
    private static String getKeyName(int keyCode) {
        if (keyCode == -1) return "NONE";

        if (!com.ji.afkcinematic.input.KeySequenceTracker.isBindableKeyCode(keyCode)) {
            return "Unsupported Key #" + keyCode;
        }

        String name = GLFW.glfwGetKeyName(keyCode, 0);
        if (name != null) return name.toUpperCase();

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
            case GLFW.GLFW_KEY_LEFT -> "←";
            case GLFW.GLFW_KEY_RIGHT -> "→";
            case GLFW.GLFW_KEY_UP -> "↑";
            case GLFW.GLFW_KEY_DOWN -> "↓";
            case GLFW.GLFW_KEY_A -> "A"; case GLFW.GLFW_KEY_B -> "B"; case GLFW.GLFW_KEY_C -> "C";
            case GLFW.GLFW_KEY_D -> "D"; case GLFW.GLFW_KEY_E -> "E"; case GLFW.GLFW_KEY_F -> "F";
            case GLFW.GLFW_KEY_G -> "G"; case GLFW.GLFW_KEY_H -> "H"; case GLFW.GLFW_KEY_I -> "I";
            case GLFW.GLFW_KEY_J -> "J"; case GLFW.GLFW_KEY_K -> "K"; case GLFW.GLFW_KEY_L -> "L";
            case GLFW.GLFW_KEY_M -> "M"; case GLFW.GLFW_KEY_N -> "N"; case GLFW.GLFW_KEY_O -> "O";
            case GLFW.GLFW_KEY_P -> "P"; case GLFW.GLFW_KEY_Q -> "Q"; case GLFW.GLFW_KEY_R -> "R";
            case GLFW.GLFW_KEY_S -> "S"; case GLFW.GLFW_KEY_T -> "T"; case GLFW.GLFW_KEY_U -> "U";
            case GLFW.GLFW_KEY_V -> "V"; case GLFW.GLFW_KEY_W -> "W"; case GLFW.GLFW_KEY_X -> "X";
            case GLFW.GLFW_KEY_Y -> "Y"; case GLFW.GLFW_KEY_Z -> "Z";
            case GLFW.GLFW_KEY_0 -> "0"; case GLFW.GLFW_KEY_1 -> "1"; case GLFW.GLFW_KEY_2 -> "2";
            case GLFW.GLFW_KEY_3 -> "3"; case GLFW.GLFW_KEY_4 -> "4"; case GLFW.GLFW_KEY_5 -> "5";
            case GLFW.GLFW_KEY_6 -> "6"; case GLFW.GLFW_KEY_7 -> "7"; case GLFW.GLFW_KEY_8 -> "8";
            case GLFW.GLFW_KEY_9 -> "9";
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
        menuKeyButton.setMessage(Text.translatable("config.ji_afkcinematic.key_waiting_first"));
    }

    private void startToggleRebind() {
        backupToggle1 = editConfig.toggleKey1; backupToggle2 = editConfig.toggleKey2;
        rebindState = RebindState.TOGGLE_WAITING_FIRST; com.ji.afkcinematic.input.KeySequenceTracker.startRebind();
        rebindStartedMs = System.currentTimeMillis();
        toggleKeyButton.setMessage(Text.translatable("config.ji_afkcinematic.key_waiting_first"));
    }

    private void cancelRebind() {
        if (rebindState == RebindState.MENU_WAITING_FIRST || rebindState == RebindState.MENU_WAITING_SECOND) {
            editConfig.menuKey1 = backupMenu1; editConfig.menuKey2 = backupMenu2;
        } else {
            editConfig.toggleKey1 = backupToggle1; editConfig.toggleKey2 = backupToggle2;
        }
        rebindState = RebindState.IDLE; com.ji.afkcinematic.input.KeySequenceTracker.resetRebind();
        refreshKeyButtonLabels();
    }

    private void refreshKeyButtonLabels() {
        menuKeyButton.setMessage(getMenuKeysText());
        toggleKeyButton.setMessage(getToggleKeysText());
    }

    private boolean isMenuDisabled() {
        return editConfig.menuKey1 == -1 && editConfig.menuKey2 == -1;
    }

    private boolean isToggleDisabled() {
        return editConfig.toggleKey1 == -1 && editConfig.toggleKey2 == -1;
    }

}
