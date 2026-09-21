package com.ji.afkcinematic.config;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ConfirmLinkScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import com.ji.afkcinematic.music.LocalMusicPackManager;
import org.lwjgl.glfw.GLFW;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

public class ConfigScreen extends Screen {
    private final Screen parent;
    private ModConfig editConfig;
    private enum RebindState { IDLE, MENU_WAITING_FIRST, MENU_WAITING_SECOND, TOGGLE_WAITING_FIRST, TOGGLE_WAITING_SECOND, IMMEDIATE_WAITING_FIRST, IMMEDIATE_WAITING_SECOND }
    private RebindState rebindState = RebindState.IDLE;
    private int backupMenu1, backupMenu2, backupToggle1, backupToggle2, backupImmediate1, backupImmediate2;
    private long rebindStartedMs = 0L;
    private ButtonWidget menuKeyButton;
    private ButtonWidget toggleKeyButton;
    private ButtonWidget immediateKeyButton;
    private ButtonWidget openMusicFolderButton;
    private ButtonWidget refreshMusicButton;
    private ButtonWidget changeMusicFolderButton;
    private ClickableWidget fishingThresholdSlider;
    private ButtonWidget reportButton;
    private ButtonWidget modEnabledButton;
    private final List<ClickableWidget> scrollableWidgets = new ArrayList<>();
    private final Map<ClickableWidget, Integer> scrollableBaseY = new IdentityHashMap<>();
    private int contentScroll;
    private int maxContentScroll;
    private static final int NORMAL_CONTENT_TOP = 80;

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
        this.editConfig.sleepLetterboxMode = current.sleepLetterboxMode;
        this.editConfig.fishingCinematicEnabled = current.fishingCinematicEnabled;
        this.editConfig.fishingCinematicThresholdSeconds = current.fishingCinematicThresholdSeconds;
        this.editConfig.chatVisibility = current.chatVisibility;
        this.editConfig.enableMusic = current.enableMusic;
        this.editConfig.musicMode = current.musicMode;
        this.editConfig.customMusicDirectory = current.customMusicDirectory;
        this.editConfig.menuKey1 = current.menuKey1;
        this.editConfig.menuKey2 = current.menuKey2;
        this.editConfig.toggleKey1 = current.toggleKey1;
        this.editConfig.toggleKey2 = current.toggleKey2;
        this.editConfig.immediateKey1 = current.immediateKey1;
        this.editConfig.immediateKey2 = current.immediateKey2;
        this.editConfig.cinematicMusicVolume = current.cinematicMusicVolume;
    }

    @Override
    protected void init() {
        scrollableWidgets.clear();
        scrollableBaseY.clear();
        int centerX = this.width / 2;
        int yLeft = getContentTop() + 5;
        int yRight = getContentTop() + 5;
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
                getMaxCyclesText(),
                (editConfig.isUnlimitedCycles() ? 20.0 : editConfig.maxCycles - 1.0) / 20.0
        ) {
            private int selectedIndex() { return (int) Math.round(this.value * 20.0); }
            @Override
            protected void updateMessage() {
                int index = selectedIndex();
                this.setMessage(Text.translatable("config.ji_afkcinematic.max_cycles",
                        index == 20 ? Text.translatable("config.ji_afkcinematic.unlimited") : Integer.toString(index + 1)));
            }
            @Override
            protected void applyValue() {
                int index = selectedIndex();
                this.value = index / 20.0;
                editConfig.maxCycles = index == 20 ? ModConfig.UNLIMITED_CYCLES : index + 1;
                updateMessage();
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
        ).tooltip(Tooltip.of(Text.translatable("config.ji_afkcinematic.tooltip.persistent_cinematics")))
         .dimensions(col2X, yRight, widgetWidth, 20).build());
        yRight += entryHeight;

        this.addDrawableChild(ButtonWidget.builder(
                getChatVisibilityText(),
                button -> {
                    editConfig.chatVisibility = editConfig.chatVisibility.next();
                    button.setMessage(getChatVisibilityText());
                }
        ).tooltip(Tooltip.of(Text.translatable("config.ji_afkcinematic.tooltip.chat_visibility")))
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
                getSleepLetterboxText(),
                button -> {
                    editConfig.sleepLetterboxMode = editConfig.sleepLetterboxMode.next();
                    button.setMessage(getSleepLetterboxText());
                }
        ).tooltip(Tooltip.of(Text.translatable("config.ji_afkcinematic.tooltip.sleep_letterbox"))).dimensions(col2X, yRight, widgetWidth, 20).build());
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

        this.addDrawableChild(ButtonWidget.builder(
                getMusicModeText(),
                button -> {
                    editConfig.musicMode = editConfig.musicMode.next();
                    button.setMessage(getMusicModeText());
                    updateMusicFolderVisibility();
                }
        ).tooltip(Tooltip.of(Text.translatable("config.ji_afkcinematic.tooltip.music_mode")))
         .dimensions(col1X, yLeft, widgetWidth, 20).build());
        yLeft += entryHeight;

        this.addDrawableChild(ButtonWidget.builder(
                Text.translatable("config.ji_afkcinematic.fishing_cinematic").append(": ")
                        .append(getOnOffText(editConfig.fishingCinematicEnabled)),
                button -> {
                    editConfig.fishingCinematicEnabled = !editConfig.fishingCinematicEnabled;
                    button.setMessage(Text.translatable("config.ji_afkcinematic.fishing_cinematic")
                            .append(": ").append(getOnOffText(editConfig.fishingCinematicEnabled)));
                    applyScrolling();
                }
        ).tooltip(Tooltip.of(Text.translatable("config.ji_afkcinematic.tooltip.fishing_cinematic")
                .append("\n").append(Text.literal("[BETA]").formatted(Formatting.YELLOW))))
                .dimensions(col1X, yLeft, widgetWidth, 20).build());
        yLeft += entryHeight;

        fishingThresholdSlider = this.addDrawableChild(new SliderWidget(
                col1X, yLeft, widgetWidth, 20,
                Text.translatable("config.ji_afkcinematic.fishing_threshold",
                        editConfig.fishingCinematicThresholdSeconds),
                editConfig.fishingCinematicThresholdSeconds / 120.0
        ) {
            private int seconds() { return (int) Math.round(this.value * 120.0); }
            @Override protected void updateMessage() {
                this.setMessage(Text.translatable("config.ji_afkcinematic.fishing_threshold", seconds()));
            }
            @Override protected void applyValue() {
                editConfig.fishingCinematicThresholdSeconds = seconds();
                updateMessage();
            }
            { setTooltip(Tooltip.of(Text.translatable("config.ji_afkcinematic.tooltip.fishing_threshold"))); }
        });
        yLeft += entryHeight;

        int centerStartY = Math.max(yLeft, yRight) + 5;

        openMusicFolderButton = ButtonWidget.builder(
                Text.translatable("config.ji_afkcinematic.open_music_folder"),
                button -> LocalMusicPackManager.openMusicFolder(editConfig.customMusicDirectory)
        ).tooltip(Tooltip.of(Text.translatable("config.ji_afkcinematic.tooltip.open_music_folder")))
         .dimensions(col1X, centerStartY, widgetWidth, 20).build();
        this.addDrawableChild(openMusicFolderButton);

        refreshMusicButton = ButtonWidget.builder(
                Text.translatable("config.ji_afkcinematic.refresh_music"),
                button -> {
                    button.active = false;
                    LocalMusicPackManager.rebuildAndReloadAsync(trackCount -> {
                        button.setMessage(Text.translatable("config.ji_afkcinematic.refresh_music_done"));
                        button.active = true;
                    });
                }
        ).tooltip(Tooltip.of(Text.translatable("config.ji_afkcinematic.tooltip.refresh_music")))
         .dimensions(col2X, centerStartY, widgetWidth, 20).build();
        this.addDrawableChild(refreshMusicButton);
        centerStartY += entryHeight;

        changeMusicFolderButton = ButtonWidget.builder(
                Text.translatable("config.ji_afkcinematic.change_music_folder"),
                button -> {
                    button.active = false;
                    LocalMusicPackManager.chooseMusicFolderAsync(editConfig.customMusicDirectory, selected -> {
                        if (selected != null) editConfig.customMusicDirectory = selected;
                        button.active = true;
                    });
                }
        ).tooltip(Tooltip.of(Text.translatable("config.ji_afkcinematic.tooltip.change_music_folder")))
         .dimensions(col1X, centerStartY, widgetWidth, 20).build();
        this.addDrawableChild(changeMusicFolderButton);
        centerStartY += entryHeight + 5;

        // Menu shortcut. Clicking it starts the normal two-key rebind flow.
        menuKeyButton = ButtonWidget.builder(
            getMenuKeysText(),
            button -> startMenuRebind()
        ).tooltip(Tooltip.of(Text.translatable("config.ji_afkcinematic.tooltip.menu_keys")))
         .dimensions(col1X, centerStartY, widgetWidth, 20).build();
        this.addDrawableChild(menuKeyButton);

        // Quick-toggle shortcut.
        toggleKeyButton = ButtonWidget.builder(
            getToggleKeysText(),
            button -> startToggleRebind()
        ).tooltip(Tooltip.of(Text.translatable("config.ji_afkcinematic.tooltip.toggle_keys")))
         .dimensions(col2X, centerStartY, widgetWidth, 20).build();
        this.addDrawableChild(toggleKeyButton);

        centerStartY += entryHeight;

        immediateKeyButton = ButtonWidget.builder(
            getImmediateKeysText(),
            button -> startImmediateRebind()
        ).tooltip(Tooltip.of(Text.translatable("config.ji_afkcinematic.tooltip.immediate_keys")))
         .dimensions(col1X, centerStartY, widgetWidth, 20).build();
        this.addDrawableChild(immediateKeyButton);

        modEnabledButton = ButtonWidget.builder(
                Text.translatable("config.ji_afkcinematic.enabled").append(": ").append(getActiveDisabledText(editConfig.modEnabled)),
                button -> {
                    editConfig.modEnabled = !editConfig.modEnabled;
                    button.setMessage(Text.translatable("config.ji_afkcinematic.enabled").append(": ").append(getActiveDisabledText(editConfig.modEnabled)));
                }
        ).tooltip(Tooltip.of(Text.translatable("config.ji_afkcinematic.tooltip.enabled"))).dimensions(col2X, centerStartY, widgetWidth, 20).build();
        this.addDrawableChild(modEnabledButton);

        captureScrollableWidgets();
        updateMusicFolderVisibility();

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
                    ModConfig current = ConfigManager.getConfig();
                    boolean musicSettingsChanged = current.musicMode != editConfig.musicMode
                            || current.enableMusic != editConfig.enableMusic
                            || current.extendedMusic != editConfig.extendedMusic
                            || !java.util.Objects.equals(current.customMusicDirectory, editConfig.customMusicDirectory);
                    editConfig.recalculate();
                    ConfigManager.setConfig(editConfig);
                    if (musicSettingsChanged) LocalMusicPackManager.rebuildAndReloadAsync(trackCount -> {});
                    this.close();
                }
        ).dimensions(centerX - 50, bottomY, 100, 20).build());

        this.addDrawableChild(ButtonWidget.builder(
                Text.translatable("config.ji_afkcinematic.cancel"),
                button -> this.close()
        ).dimensions(centerX + 55, bottomY, 100, 20).build());
        configureScrolling();
    }

    private void captureScrollableWidgets() {
        for (Object child : this.children()) {
            if (child instanceof ClickableWidget widget) {
                scrollableWidgets.add(widget);
                scrollableBaseY.put(widget, widget.getY());
            }
        }
    }

    private void configureScrolling() {
        int viewportBottom = this.height - 45;
        int contentBottom = getContentTop();
        for (ClickableWidget widget : scrollableWidgets) {
            contentBottom = Math.max(contentBottom, scrollableBaseY.get(widget) + widget.getHeight());
        }
        maxContentScroll = Math.max(0, contentBottom - viewportBottom + 4);
        contentScroll = Math.max(0, Math.min(contentScroll, maxContentScroll));
        applyScrolling();
    }

    private void applyScrolling() {
        int viewportBottom = this.height - 45;
        for (ClickableWidget widget : scrollableWidgets) {
            int y = scrollableBaseY.get(widget) - contentScroll;
            widget.setY(y);
            boolean inside = y >= getContentTop() && y + widget.getHeight() <= viewportBottom;
            boolean customMusicControl = widget == openMusicFolderButton || widget == refreshMusicButton
                    || widget == changeMusicFolderButton;
            boolean fishingControl = widget == fishingThresholdSlider;
            widget.visible = inside
                    && (!customMusicControl || editConfig.musicMode.includesCustom())
                    && (!fishingControl || editConfig.fishingCinematicEnabled);
            widget.active = widget.visible;
        }
    }

    private int getContentTop() {
        return this.height < 360 ? 46 : NORMAL_CONTENT_TOP;
    }

    private void updateMusicFolderVisibility() {
        applyScrolling();
    }

    public void scrollContent(double vertical) {
        if (maxContentScroll <= 0) return;
        contentScroll = Math.max(0, Math.min(maxContentScroll,
                contentScroll - (int) Math.round(vertical * 22.0)));
        applyScrolling();
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

    private Text getSleepLetterboxText() {
        String key = "config.ji_afkcinematic.sleep_letterbox_mode."
                + editConfig.sleepLetterboxMode.name().toLowerCase();
        Formatting color = switch (editConfig.sleepLetterboxMode) {
            case DISABLED -> Formatting.GREEN;
            case MODERATE -> Formatting.YELLOW;
            case COMPLETE -> Formatting.GOLD;
        };
        return Text.translatable("config.ji_afkcinematic.sleep_letterbox")
                .append(": ").append(Text.translatable(key).formatted(color));
    }

    private Text getMaxCyclesText() {
        Object value = editConfig.isUnlimitedCycles()
                ? Text.translatable("config.ji_afkcinematic.unlimited")
                : Integer.toString(editConfig.maxCycles);
        return Text.translatable("config.ji_afkcinematic.max_cycles", value);
    }

    private Text getChatVisibilityText() {
        String key = "config.ji_afkcinematic.chat_visibility."
                + editConfig.chatVisibility.name().toLowerCase();
        Formatting color = editConfig.chatVisibility == CinematicChatVisibility.VISIBLE
                ? Formatting.YELLOW : Formatting.RED;
        return Text.translatable("config.ji_afkcinematic.chat_visibility")
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

    private Text getImmediateKeysText() {
        return Text.empty()
                .append(Text.translatable("config.ji_afkcinematic.immediate_keys.label").formatted(Formatting.WHITE))
                .append(isImmediateDisabled()
                        ? Text.translatable("config.ji_afkcinematic.keybind_disabled_label")
                        : Text.literal(formatKeys(editConfig.immediateKey1, editConfig.immediateKey2)).formatted(Formatting.YELLOW));
    }

    private Text getMusicModeText() {
        Formatting color = switch (editConfig.musicMode) {
            case VANILLA -> Formatting.GREEN;
            case MIXED -> Formatting.GOLD;
            case CUSTOM -> Formatting.YELLOW;
        };
        return Text.translatable("config.ji_afkcinematic.music_mode").append(": ")
                .append(Text.translatable("config.ji_afkcinematic.music_mode." + editConfig.musicMode.name().toLowerCase()).formatted(color));
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

        if (maxContentScroll > 0) {
            int top = getContentTop();
            int bottom = this.height - 45;
            int trackX = this.width / 2 + 150;
            int trackHeight = bottom - top;
            int thumbHeight = Math.max(18, trackHeight * trackHeight / (trackHeight + maxContentScroll));
            int thumbY = top + (trackHeight - thumbHeight) * contentScroll / maxContentScroll;
            context.fill(trackX, top, trackX + 3, bottom, 0x55333333);
            context.fill(trackX, thumbY, trackX + 3, thumbY + thumbHeight, 0xFFAAAAAA);
        }

        if (this.reportButton != null) {
            long time = System.currentTimeMillis() / 800;
            int phase = (int) (time % 3);
            if (phase == 0) this.reportButton.setMessage(Text.literal("§e⚠"));
            else if (phase == 1) this.reportButton.setMessage(Text.literal("§b♦"));
            else this.reportButton.setMessage(Text.literal("§a✉"));
        }

        int titleY = this.height < 360 ? 17 : 55;
        context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("§6§lJi AFK Cinematic"), this.width / 2, titleY, 0xFFFFFFFF);
        context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("§5By jiory_"), this.width / 2, titleY + 10, 0xFFFFFFFF);
        context.drawTextWithShadow(this.textRenderer, Text.translatable("config.ji_afkcinematic.footer"), 8, this.height - 12, 0xFFD8A7FF);
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
                if (rebindState == RebindState.MENU_WAITING_FIRST || rebindState == RebindState.TOGGLE_WAITING_FIRST || rebindState == RebindState.IMMEDIATE_WAITING_FIRST) {
                    if (rebindState == RebindState.MENU_WAITING_FIRST) {
                        editConfig.menuKey1 = -1;
                        editConfig.menuKey2 = -1;
                    } else if (rebindState == RebindState.TOGGLE_WAITING_FIRST) {
                        editConfig.toggleKey1 = -1;
                        editConfig.toggleKey2 = -1;
                    } else {
                        editConfig.immediateKey1 = -1;
                        editConfig.immediateKey2 = -1;
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
            if (rebindState == RebindState.MENU_WAITING_FIRST || rebindState == RebindState.TOGGLE_WAITING_FIRST || rebindState == RebindState.IMMEDIATE_WAITING_FIRST) {
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
                ButtonWidget target = (rebindState == RebindState.MENU_WAITING_FIRST || rebindState == RebindState.MENU_WAITING_SECOND)
                                  ? menuKeyButton : (rebindState == RebindState.TOGGLE_WAITING_FIRST || rebindState == RebindState.TOGGLE_WAITING_SECOND)
                                  ? toggleKeyButton : immediateKeyButton;
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
            } else if (rebindState == RebindState.TOGGLE_WAITING_FIRST || rebindState == RebindState.TOGGLE_WAITING_SECOND) {
                if (result == 1) {
                    rebindState = RebindState.TOGGLE_WAITING_SECOND;
                    toggleKeyButton.setMessage(Text.translatable("config.ji_afkcinematic.key_waiting_second"));
                } else if (result == 2) {
                    editConfig.toggleKey1 = out[0];
                    editConfig.toggleKey2 = out[1];
                    rebindState = RebindState.IDLE; com.ji.afkcinematic.input.KeySequenceTracker.resetRebind(); refreshKeyButtonLabels();

                }
            } else {
                if (result == 1) {
                    rebindState = RebindState.IMMEDIATE_WAITING_SECOND;
                    immediateKeyButton.setMessage(Text.translatable("config.ji_afkcinematic.key_waiting_second"));
                } else if (result == 2) {
                    editConfig.immediateKey1 = out[0];
                    editConfig.immediateKey2 = out[1];
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

    private void startImmediateRebind() {
        backupImmediate1 = editConfig.immediateKey1; backupImmediate2 = editConfig.immediateKey2;
        rebindState = RebindState.IMMEDIATE_WAITING_FIRST; com.ji.afkcinematic.input.KeySequenceTracker.startRebind();
        rebindStartedMs = System.currentTimeMillis();
        immediateKeyButton.setMessage(Text.translatable("config.ji_afkcinematic.key_waiting_first"));
    }

    private void cancelRebind() {
        if (rebindState == RebindState.MENU_WAITING_FIRST || rebindState == RebindState.MENU_WAITING_SECOND) {
            editConfig.menuKey1 = backupMenu1; editConfig.menuKey2 = backupMenu2;
        } else if (rebindState == RebindState.TOGGLE_WAITING_FIRST || rebindState == RebindState.TOGGLE_WAITING_SECOND) {
            editConfig.toggleKey1 = backupToggle1; editConfig.toggleKey2 = backupToggle2;
        } else {
            editConfig.immediateKey1 = backupImmediate1; editConfig.immediateKey2 = backupImmediate2;
        }
        rebindState = RebindState.IDLE; com.ji.afkcinematic.input.KeySequenceTracker.resetRebind();
        refreshKeyButtonLabels();
    }

    private void refreshKeyButtonLabels() {
        menuKeyButton.setMessage(getMenuKeysText());
        toggleKeyButton.setMessage(getToggleKeysText());
        immediateKeyButton.setMessage(getImmediateKeysText());
    }

    private boolean isMenuDisabled() {
        return editConfig.menuKey1 == -1 && editConfig.menuKey2 == -1;
    }

    private boolean isToggleDisabled() {
        return editConfig.toggleKey1 == -1 && editConfig.toggleKey2 == -1;
    }

    private boolean isImmediateDisabled() {
        return editConfig.immediateKey1 == -1 && editConfig.immediateKey2 == -1;
    }

}
