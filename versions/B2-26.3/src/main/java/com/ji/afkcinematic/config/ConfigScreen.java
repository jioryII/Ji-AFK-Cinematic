package com.ji.afkcinematic.config;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import com.mojang.blaze3d.platform.InputConstants;
import com.ji.afkcinematic.music.LocalMusicPackManager;
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
    private Button menuKeyButton;
    private Button toggleKeyButton;
    private Button immediateKeyButton;
    private Button openMusicFolderButton;
    private Button refreshMusicButton;
    private Button changeMusicFolderButton;
    private AbstractWidget fishingThresholdSlider;

    private Button reportButton;
    private final List<AbstractWidget> scrollableWidgets = new ArrayList<>();
    private final Map<AbstractWidget, Integer> scrollableBaseY = new IdentityHashMap<>();
    private int contentScroll;
    private int maxContentScroll;
    private static final int NORMAL_CONTENT_TOP = 80;
    private Button modEnabledButton;

    // Estado para restaurar el label tras UNSUPPORTED_KEY_REJECTED.
    private Button pendingLabelTarget;
    private Component pendingLabelOriginal;
    private int pendingLabelTicksRemaining;

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
                getMaxCyclesText(),
                (editConfig.isUnlimitedCycles() ? 20.0 : editConfig.maxCycles - 1.0) / 20.0
        ) {
            private int selectedIndex() { return (int) Math.round(this.value * 20.0); }
            @Override
            protected void updateMessage() {
                int index = selectedIndex();
                this.setMessage(Component.translatable("config.ji_afkcinematic.max_cycles",
                        index == 20 ? Component.translatable("config.ji_afkcinematic.unlimited") : Integer.toString(index + 1)));
            }
            @Override
            protected void applyValue() {
                int index = selectedIndex();
                this.value = index / 20.0;
                editConfig.maxCycles = index == 20 ? ModConfig.UNLIMITED_CYCLES : index + 1;
                updateMessage();
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

        this.addRenderableWidget(new AbstractSliderButton(
                col1X, yLeft, widgetWidth, 20,
                Component.translatable("config.ji_afkcinematic.shot_mix",
                        editConfig.characterShotPercentage, 100 - editConfig.characterShotPercentage),
                editConfig.characterShotPercentage / 100.0
        ) {
            private int snappedValue() { return (int) Math.round(this.value * 10.0) * 10; }
            @Override
            protected void updateMessage() {
                int val = snappedValue();
                this.setMessage(Component.translatable("config.ji_afkcinematic.shot_mix", val, 100 - val));
            }
            @Override
            protected void applyValue() {
                int val = snappedValue();
                this.value = val / 100.0;
                editConfig.characterShotPercentage = val;
                updateMessage();
            }
            { setTooltip(Tooltip.create(Component.translatable("config.ji_afkcinematic.tooltip.shot_mix"))); }
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
                getPersistentModeText(),
                button -> {
                    editConfig.persistentMode = editConfig.persistentMode.next();
                    button.setMessage(getPersistentModeText());
                }
        ).tooltip(Tooltip.create(Component.translatable("config.ji_afkcinematic.tooltip.persistent_cinematics")))
         .bounds(col2X, yRight, widgetWidth, 20).build());
        yRight += entryHeight;

        this.addRenderableWidget(Button.builder(
                getChatVisibilityText(),
                button -> {
                    editConfig.chatVisibility = editConfig.chatVisibility.next();
                    button.setMessage(getChatVisibilityText());
                }
        ).tooltip(Tooltip.create(Component.translatable("config.ji_afkcinematic.tooltip.chat_visibility")))
         .bounds(col2X, yRight, widgetWidth, 20).build());
        yRight += entryHeight;

        this.addRenderableWidget(Button.builder(
                Component.translatable("config.ji_afkcinematic.camera_rotation")
                        .append(": ").append(getOnOffText(editConfig.cameraRotationEnabled)),
                button -> {
                    editConfig.cameraRotationEnabled = !editConfig.cameraRotationEnabled;
                    button.setMessage(Component.translatable("config.ji_afkcinematic.camera_rotation")
                            .append(": ").append(getOnOffText(editConfig.cameraRotationEnabled)));
                }
        ).tooltip(Tooltip.create(Component.translatable("config.ji_afkcinematic.tooltip.camera_rotation")))
         .bounds(col2X, yRight, widgetWidth, 20).build());
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
                getSleepLetterboxText(),
                button -> {
                    editConfig.sleepLetterboxMode = editConfig.sleepLetterboxMode.next();
                    button.setMessage(getSleepLetterboxText());
                }
        ).tooltip(Tooltip.create(Component.translatable("config.ji_afkcinematic.tooltip.sleep_letterbox")))
                .bounds(col2X, yRight, widgetWidth, 20).build());
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

        this.addRenderableWidget(Button.builder(
                getMusicModeText(),
                button -> {
                    editConfig.musicMode = editConfig.musicMode.next();
                    button.setMessage(getMusicModeText());
                    updateMusicFolderVisibility();
                }
        ).tooltip(Tooltip.create(Component.translatable("config.ji_afkcinematic.tooltip.music_mode")))
         .bounds(col1X, yLeft, widgetWidth, 20).build());
        yLeft += entryHeight;

        this.addRenderableWidget(Button.builder(
                Component.translatable("config.ji_afkcinematic.fishing_cinematic").append(": ")
                        .append(getOnOffText(editConfig.fishingCinematicEnabled)),
                button -> {
                    editConfig.fishingCinematicEnabled = !editConfig.fishingCinematicEnabled;
                    button.setMessage(Component.translatable("config.ji_afkcinematic.fishing_cinematic")
                            .append(": ").append(getOnOffText(editConfig.fishingCinematicEnabled)));
                    applyScrolling();
                }
        ).tooltip(Tooltip.create(Component.translatable("config.ji_afkcinematic.tooltip.fishing_cinematic")
                .append("\n").append(Component.literal("[BETA]").withStyle(ChatFormatting.YELLOW))))
                .bounds(col1X, yLeft, widgetWidth, 20).build());
        yLeft += entryHeight;

        fishingThresholdSlider = this.addRenderableWidget(new AbstractSliderButton(
                col1X, yLeft, widgetWidth, 20,
                Component.translatable("config.ji_afkcinematic.fishing_threshold",
                        editConfig.fishingCinematicThresholdSeconds),
                editConfig.fishingCinematicThresholdSeconds / 120.0
        ) {
            private int seconds() { return (int) Math.round(this.value * 120.0); }
            @Override protected void updateMessage() {
                this.setMessage(Component.translatable("config.ji_afkcinematic.fishing_threshold", seconds()));
            }
            @Override protected void applyValue() {
                editConfig.fishingCinematicThresholdSeconds = seconds();
                updateMessage();
            }
            { setTooltip(Tooltip.create(Component.translatable("config.ji_afkcinematic.tooltip.fishing_threshold"))); }
        });
        yLeft += entryHeight;

        int centerStartY = Math.max(yLeft, yRight) + 5;

        openMusicFolderButton = Button.builder(
                Component.translatable("config.ji_afkcinematic.open_music_folder"),
                button -> LocalMusicPackManager.openMusicFolder(editConfig.customMusicDirectory)
        ).tooltip(Tooltip.create(Component.translatable("config.ji_afkcinematic.tooltip.open_music_folder")))
         .bounds(col1X, centerStartY, widgetWidth, 20).build();
        this.addRenderableWidget(openMusicFolderButton);

        refreshMusicButton = Button.builder(
                Component.translatable("config.ji_afkcinematic.refresh_music"),
                button -> {
                    button.active = false;
                    LocalMusicPackManager.rebuildAndReloadAsync(trackCount -> {
                        button.setMessage(Component.translatable("config.ji_afkcinematic.refresh_music_done"));
                        button.active = true;
                    });
                }
        ).tooltip(Tooltip.create(Component.translatable("config.ji_afkcinematic.tooltip.refresh_music")))
         .bounds(col2X, centerStartY, widgetWidth, 20).build();
        this.addRenderableWidget(refreshMusicButton);
        centerStartY += entryHeight;

        changeMusicFolderButton = Button.builder(
                Component.translatable("config.ji_afkcinematic.change_music_folder"),
                button -> {
                    button.active = false;
                    LocalMusicPackManager.chooseMusicFolderAsync(editConfig.customMusicDirectory, selected -> {
                        if (selected != null) editConfig.customMusicDirectory = selected;
                        button.active = true;
                    });
                }
        ).tooltip(Tooltip.create(Component.translatable("config.ji_afkcinematic.tooltip.change_music_folder")))
         .bounds(col1X, centerStartY, widgetWidth, 20).build();
        this.addRenderableWidget(changeMusicFolderButton);
        centerStartY += entryHeight + 5;

        // Menu shortcut. Clicking it starts the normal two-key rebind flow.
        menuKeyButton = Button.builder(
            getMenuKeysText(),
            button -> startMenuRebind()
        ).tooltip(Tooltip.create(Component.translatable("config.ji_afkcinematic.tooltip.menu_keys")))
         .bounds(col1X, centerStartY, widgetWidth, 20).build();
        this.addRenderableWidget(menuKeyButton);

        // Quick-toggle shortcut.
        toggleKeyButton = Button.builder(
            getToggleKeysText(),
            button -> startToggleRebind()
        ).tooltip(Tooltip.create(Component.translatable("config.ji_afkcinematic.tooltip.toggle_keys")))
         .bounds(col2X, centerStartY, widgetWidth, 20).build();
        this.addRenderableWidget(toggleKeyButton);

        centerStartY += entryHeight;

        immediateKeyButton = Button.builder(
            getImmediateKeysText(), button -> startImmediateRebind()
        ).tooltip(Tooltip.create(Component.translatable("config.ji_afkcinematic.tooltip.immediate_keys")))
         .bounds(col1X, centerStartY, widgetWidth, 20).build();
        this.addRenderableWidget(immediateKeyButton);

        modEnabledButton = Button.builder(
                Component.translatable("config.ji_afkcinematic.enabled").append(": ").append(getActiveDisabledText(editConfig.modEnabled)),
                button -> {
                    editConfig.modEnabled = !editConfig.modEnabled;
                    button.setMessage(Component.translatable("config.ji_afkcinematic.enabled").append(": ").append(getActiveDisabledText(editConfig.modEnabled)));
                }
        ).tooltip(Tooltip.create(Component.translatable("config.ji_afkcinematic.tooltip.enabled"))).bounds(col2X, centerStartY, widgetWidth, 20).build();
        this.addRenderableWidget(modEnabledButton);

        captureScrollableWidgets();
        updateMusicFolderVisibility();

        this.reportButton = Button.builder(
                Component.literal("§e⚠"),
                ConfirmLinkScreen.confirmLink(this, java.net.URI.create("https://discord.gg/sE27D5SNaq"))
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
                    ModConfig current = ConfigManager.getConfig();
                    boolean musicSettingsChanged = current.musicMode != editConfig.musicMode
                            || current.enableMusic != editConfig.enableMusic
                            || current.extendedMusic != editConfig.extendedMusic
                            || !java.util.Objects.equals(current.customMusicDirectory, editConfig.customMusicDirectory);
                    editConfig.recalculate();
                    ConfigManager.setConfig(editConfig);
                    if (musicSettingsChanged) LocalMusicPackManager.rebuildAndReloadAsync(trackCount -> {});
                    this.onClose();
                }
        ).bounds(centerX - 50, bottomY, 100, 20).build());

        this.addRenderableWidget(Button.builder(
                Component.translatable("config.ji_afkcinematic.cancel"),
                button -> this.onClose()
        ).bounds(centerX + 55, bottomY, 100, 20).build());
        configureScrolling();
    }

    private void captureScrollableWidgets() {
        for (Object child : this.children()) {
            if (child instanceof AbstractWidget widget) {
                scrollableWidgets.add(widget);
                scrollableBaseY.put(widget, widget.getY());
            }
        }
    }

    private void configureScrolling() {
        int viewportBottom = this.height - 45;
        int contentBottom = getContentTop();
        for (AbstractWidget widget : scrollableWidgets) {
            contentBottom = Math.max(contentBottom, scrollableBaseY.get(widget) + widget.getHeight());
        }
        maxContentScroll = Math.max(0, contentBottom - viewportBottom + 4);
        contentScroll = Math.max(0, Math.min(contentScroll, maxContentScroll));
        applyScrolling();
    }

    private void applyScrolling() {
        int viewportBottom = this.height - 45;
        for (AbstractWidget widget : scrollableWidgets) {
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

    private Component getDamageActionText() {
        return Component.translatable("config.ji_afkcinematic.damage_action")
                .append(": ")
                .append(Component.translatable("config.ji_afkcinematic.damage_action." + editConfig.damageAction.name().toLowerCase()));
    }

    private Component getPersistentModeText() {
        ChatFormatting color = switch (editConfig.persistentMode) {
            case NORMAL -> ChatFormatting.GRAY;
            case INTERACTIVE -> ChatFormatting.YELLOW;
            case PERSISTENT -> ChatFormatting.RED;
        };
        String key = "config.ji_afkcinematic.persistent_mode."
                + editConfig.persistentMode.name().toLowerCase();
        return Component.translatable("config.ji_afkcinematic.persistent_cinematics")
                .append(": ").append(Component.translatable(key).withStyle(color));
    }

    private Component getSleepLetterboxText() {
        String key = "config.ji_afkcinematic.sleep_letterbox_mode."
                + editConfig.sleepLetterboxMode.name().toLowerCase();
        ChatFormatting color = switch (editConfig.sleepLetterboxMode) {
            case DISABLED -> ChatFormatting.GREEN;
            case MODERATE -> ChatFormatting.YELLOW;
            case COMPLETE -> ChatFormatting.GOLD;
        };
        return Component.translatable("config.ji_afkcinematic.sleep_letterbox")
                .append(": ").append(Component.translatable(key).withStyle(color));
    }

    private Component getMaxCyclesText() {
        Object value = editConfig.isUnlimitedCycles()
                ? Component.translatable("config.ji_afkcinematic.unlimited")
                : Integer.toString(editConfig.maxCycles);
        return Component.translatable("config.ji_afkcinematic.max_cycles", value);
    }

    private Component getChatVisibilityText() {
        String key = "config.ji_afkcinematic.chat_visibility."
                + editConfig.chatVisibility.name().toLowerCase();
        ChatFormatting color = editConfig.chatVisibility == CinematicChatVisibility.VISIBLE
                ? ChatFormatting.YELLOW : ChatFormatting.RED;
        return Component.translatable("config.ji_afkcinematic.chat_visibility")
                .append(": ").append(Component.translatable(key).withStyle(color));
    }

    private Component getMenuKeysText() {
        if (isMenuDisabled()) {
            return Component.empty()
                    .append(Component.translatable("config.ji_afkcinematic.menu_keys.label").withStyle(ChatFormatting.WHITE))
                    .append(Component.translatable("config.ji_afkcinematic.keybind_disabled_label"));
        }
        String keys = formatKeys(editConfig.menuKey1, editConfig.menuKey2);
        return Component.empty()
                .append(Component.translatable("config.ji_afkcinematic.menu_keys.label").withStyle(ChatFormatting.WHITE))
                .append(Component.literal(keys).withStyle(ChatFormatting.YELLOW));
    }

    private Component getToggleKeysText() {
        if (isToggleDisabled()) {
            return Component.empty()
                    .append(Component.translatable("config.ji_afkcinematic.toggle_keys.label").withStyle(ChatFormatting.WHITE))
                    .append(Component.translatable("config.ji_afkcinematic.keybind_disabled_label"));
        }
        String keys = formatKeys(editConfig.toggleKey1, editConfig.toggleKey2);
        return Component.empty()
                .append(Component.translatable("config.ji_afkcinematic.toggle_keys.label").withStyle(ChatFormatting.WHITE))
                .append(Component.literal(keys).withStyle(ChatFormatting.YELLOW));
    }

    private Component getImmediateKeysText() {
        return Component.empty()
                .append(Component.translatable("config.ji_afkcinematic.immediate_keys.label").withStyle(ChatFormatting.WHITE))
                .append(isImmediateDisabled() ? Component.translatable("config.ji_afkcinematic.keybind_disabled_label")
                        : Component.literal(formatKeys(editConfig.immediateKey1, editConfig.immediateKey2)).withStyle(ChatFormatting.YELLOW));
    }

    private Component getMusicModeText() {
        ChatFormatting color = switch (editConfig.musicMode) {
            case VANILLA -> ChatFormatting.GREEN;
            case MIXED -> ChatFormatting.GOLD;
            case CUSTOM -> ChatFormatting.YELLOW;
        };
        return Component.translatable("config.ji_afkcinematic.music_mode").append(": ")
                .append(Component.translatable("config.ji_afkcinematic.music_mode." + editConfig.musicMode.name().toLowerCase()).withStyle(color));
    }

    private Component getActiveDisabledText(boolean value) {
        if (value) return Component.literal("§a").append(Component.translatable("config.ji_afkcinematic.active"));
        return Component.literal("§c").append(Component.translatable("config.ji_afkcinematic.disabled"));
    }

    private Component getOnOffText(boolean value) {
        if (value) return Component.literal("§a").append(Component.translatable("config.ji_afkcinematic.on"));
        return Component.literal("§c").append(Component.translatable("config.ji_afkcinematic.off"));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        super.extractRenderState(context, mouseX, mouseY, delta);

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
            if (phase == 0) this.reportButton.setMessage(Component.literal("§e⚠"));
            else if (phase == 1) this.reportButton.setMessage(Component.literal("§b♦"));
            else this.reportButton.setMessage(Component.literal("§a✉"));
        }

        int titleY = this.height < 360 ? 17 : 55;
        context.centeredText(this.font, Component.literal("§6§lJi AFK Cinematic"), this.width / 2, titleY, 0xFFFFFFFF);
        context.centeredText(this.font, Component.literal("§5By jiory_"), this.width / 2, titleY + 10, 0xFFFFFFFF);
        context.text(this.font, Component.translatable("config.ji_afkcinematic.footer"), 8, this.height - 12, 0xFFD8A7FF);
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
    public boolean keyPressed(KeyEvent event) {
        int keyCode = event.key();
        if (rebindState == RebindState.IDLE && keyCode == InputConstants.KEY_ESCAPE) {
            this.onClose();
            return true;
        }
        if (rebindState != RebindState.IDLE) {
            // ESC durante rebind
            if (keyCode == InputConstants.KEY_ESCAPE) {
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
            // Timeout
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

            // Unknown and out-of-range native codes are not persisted.
            if (result == com.ji.afkcinematic.input.KeySequenceTracker.UNSUPPORTED_KEY_REJECTED) {
                Button target = (rebindState == RebindState.MENU_WAITING_FIRST || rebindState == RebindState.MENU_WAITING_SECOND)
                                  ? menuKeyButton : (rebindState == RebindState.TOGGLE_WAITING_FIRST || rebindState == RebindState.TOGGLE_WAITING_SECOND)
                                  ? toggleKeyButton : immediateKeyButton;
                pendingLabelTarget = target;
                pendingLabelOriginal = target.getMessage();
                target.setMessage(Component.literal("Unsupported key")
                    .withStyle(ChatFormatting.RED));
                pendingLabelTicksRemaining = 30;
                return true;
            }

            if (rebindState == RebindState.MENU_WAITING_FIRST || rebindState == RebindState.MENU_WAITING_SECOND) {
                if (result == 1) {
                    rebindState = RebindState.MENU_WAITING_SECOND;
                    menuKeyButton.setMessage(Component.translatable("config.ji_afkcinematic.key_waiting_second"));
                } else if (result == 2) {
                    editConfig.menuKey1 = out[0];
                    editConfig.menuKey2 = out[1];
                    rebindState = RebindState.IDLE; com.ji.afkcinematic.input.KeySequenceTracker.resetRebind(); refreshKeyButtonLabels();
                }
            } else if (rebindState == RebindState.TOGGLE_WAITING_FIRST || rebindState == RebindState.TOGGLE_WAITING_SECOND) {
                if (result == 1) {
                    rebindState = RebindState.TOGGLE_WAITING_SECOND;
                    toggleKeyButton.setMessage(Component.translatable("config.ji_afkcinematic.key_waiting_second"));
                } else if (result == 2) {
                    editConfig.toggleKey1 = out[0];
                    editConfig.toggleKey2 = out[1];
                    rebindState = RebindState.IDLE; com.ji.afkcinematic.input.KeySequenceTracker.resetRebind(); refreshKeyButtonLabels();
                }
            } else {
                if (result == 1) {
                    rebindState = RebindState.IMMEDIATE_WAITING_SECOND;
                    immediateKeyButton.setMessage(Component.translatable("config.ji_afkcinematic.key_waiting_second"));
                } else if (result == 2) {
                    editConfig.immediateKey1 = out[0]; editConfig.immediateKey2 = out[1];
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

    /** Display names come from the active vanilla input backend. */
    private static String getKeyName(int keyCode) {
        if (keyCode == -1) return "NONE";
        if (!com.ji.afkcinematic.input.KeySequenceTracker.isBindableKeyCode(keyCode))
            return "Unsupported Key #" + keyCode;
        return InputConstants.Type.KEYBOARD.getOrCreate(keyCode).getDisplayName().getString();
    }

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

    private void startImmediateRebind() {
        backupImmediate1 = editConfig.immediateKey1; backupImmediate2 = editConfig.immediateKey2;
        rebindState = RebindState.IMMEDIATE_WAITING_FIRST; com.ji.afkcinematic.input.KeySequenceTracker.startRebind();
        rebindStartedMs = System.currentTimeMillis();
        immediateKeyButton.setMessage(Component.translatable("config.ji_afkcinematic.key_waiting_first"));
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

    // === Hardening v2.2.2: helpers Disable/Re-bind ===

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
