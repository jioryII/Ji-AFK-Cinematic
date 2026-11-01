package com.ji.afkcinematic.config;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
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
import java.util.Set;
import java.util.Collections;

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
    private Button refreshMusicButton;
    private Button manageMusicButton;
    private AbstractWidget fishingThresholdSlider;
    private AbstractWidget focusSlider;

    private Button reportButton;
    private Button discordCornerButton;
    private Button websiteCornerButton;
    private final List<AbstractWidget> scrollableWidgets = new ArrayList<>();
    private final Map<AbstractWidget, Integer> scrollableBaseY = new IdentityHashMap<>();
    private final Map<AbstractWidget, Boolean> legacyLeftColumn = new IdentityHashMap<>();
    private int contentScroll;
    private int maxContentScroll;
    private static final int NORMAL_CONTENT_TOP = 96;
    private enum Category {
        ALL("all"), GENERAL("general"), AFK("afk"), CAMERA("camera"), SLEEP("sleep"),
        MUSIC("music"), CONTROLS("controls"), SUPPORT("support");
        private final String key;
        Category(String key) { this.key = key; }
    }
    private Category selectedCategory = Category.ALL;
    private final Map<AbstractWidget, Category> widgetCategories = new IdentityHashMap<>();
    private final Set<AbstractWidget> busyWidgets = Collections.newSetFromMap(new IdentityHashMap<>());
    private Button resetButton;
    private Button saveButton;
    private boolean saving;
    private boolean awaitingConfig;
    private int resetConfirmationTicks;
    private final com.ji.afkcinematic.client.UiWorkScope uiWork = new com.ji.afkcinematic.client.UiWorkScope();

    private Button modEnabledButton;

    // Estado para restaurar el label tras UNSUPPORTED_KEY_REJECTED.
    private Button pendingLabelTarget;
    private Component pendingLabelOriginal;
    private int pendingLabelTicksRemaining;

    public ConfigScreen(Screen parent) {
        super(Component.translatable("config.ji_afkcinematic.title"));
        this.parent = parent;
        cloneConfig(); awaitingConfig = ConfigManager.isLoading();
    }

    private void cloneConfig() {
        ModConfig current = ConfigManager.getConfig();
        this.editConfig = new ModConfig();
        this.editConfig.shotDurationSeconds = current.shotDurationSeconds;
        this.editConfig.afkThresholdSeconds = current.afkThresholdSeconds;
        this.editConfig.maxCycles = current.maxCycles;
        this.editConfig.cameraSpeed = current.cameraSpeed;
        this.editConfig.focusLevel = current.focusLevel;
        this.editConfig.characterShotPercentage = current.characterShotPercentage;
        this.editConfig.persistentMode = current.persistentMode;
        this.editConfig.chatVisibility = current.chatVisibility;
        this.editConfig.cameraRotationEnabled = current.cameraRotationEnabled;
        this.editConfig.damageAction = current.damageAction;
        this.editConfig.extendedMusic = current.extendedMusic;
        this.editConfig.modEnabled = current.modEnabled;
        this.editConfig.enableLetterbox = current.enableLetterbox;
        this.editConfig.sleepLetterboxMode = current.sleepLetterboxMode;
        this.editConfig.fishingCinematicEnabled = current.fishingCinematicEnabled;
        this.editConfig.fishingCinematicThresholdSeconds = current.fishingCinematicThresholdSeconds;
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
        this.editConfig.configVersion = current.configVersion;
        this.editConfig.menuKey1Name = current.menuKey1Name;
        this.editConfig.menuKey2Name = current.menuKey2Name;
        this.editConfig.toggleKey1Name = current.toggleKey1Name;
        this.editConfig.toggleKey2Name = current.toggleKey2Name;
        this.editConfig.immediateKey1Name = current.immediateKey1Name;
        this.editConfig.immediateKey2Name = current.immediateKey2Name;
    }

    @Override
    protected void init() {
        uiWork.begin();
        widgetCategories.clear();
        busyWidgets.clear();
        resetConfirmationTicks = 0;
        pendingLabelTarget = null;
        pendingLabelOriginal = null;
        pendingLabelTicksRemaining = 0;
        scrollableWidgets.clear();
        scrollableBaseY.clear();
        legacyLeftColumn.clear();
        int centerX = this.width / 2;
        int yLeft = getContentTop() + 5;
        int yRight = getContentTop() + 5;
        int widgetWidth = 135;
        int entryHeight = 26;

        int col1X = centerX - 140;
        int col2X = centerX + 5;

        addOption(new AbstractSliderButton(
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
        }, Category.MUSIC);
        yLeft += entryHeight;

        addOption(new AbstractSliderButton(
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
        }, Category.CAMERA);
        yLeft += entryHeight;

        addOption(new AbstractSliderButton(
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
        }, Category.AFK);
        yLeft += entryHeight;

        addOption(new AbstractSliderButton(
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
        }, Category.CAMERA);
        yLeft += entryHeight;

        addOption(new AbstractSliderButton(
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
        }, Category.CAMERA);
        yLeft += entryHeight;

        addOption(new AbstractSliderButton(
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
        }, Category.CAMERA);
        yLeft += entryHeight;

        focusSlider = null;
        if (com.ji.afkcinematic.cinematic.FocusController.AVAILABLE) {
            focusSlider = addOption(new AbstractSliderButton(col1X, yLeft, widgetWidth, 20,
                    Component.translatable("config.ji_afkcinematic.focus_level", editConfig.focusLevel), editConfig.focusLevel / 4.0) {
                @Override protected void updateMessage() {
                    setMessage(Component.translatable("config.ji_afkcinematic.focus_level", (int)Math.round(value * 4)));
                }
                @Override protected void applyValue() {
                    editConfig.focusLevel = (int)Math.round(value * 4);
                    value = editConfig.focusLevel / 4.0; updateMessage();
                }
                @Override public boolean keyPressed(KeyEvent event) {
                    if (event.key() == InputConstants.KEY_LEFT || event.key() == InputConstants.KEY_RIGHT) {
                        int step = event.key() == InputConstants.KEY_LEFT ? -1 : 1;
                        value = Math.max(0, Math.min(4, editConfig.focusLevel + step)) / 4.0;
                        applyValue(); return true;
                    }
                    return super.keyPressed(event);
                }
            }, Category.CAMERA);
            updateFocusTooltip();
            yLeft += entryHeight;
        }

        addOption(Button.builder(
                getDamageActionText(),
                button -> {
                    int nextOrdinal = (editConfig.damageAction.ordinal() + 1) % DamageAction.values().length;
                    editConfig.damageAction = DamageAction.values()[nextOrdinal];
                    button.setMessage(getDamageActionText());
                }
        ).tooltip(Tooltip.create(Component.translatable("config.ji_afkcinematic.tooltip.damage_action"))).bounds(col2X, yRight, widgetWidth, 20).build(), Category.GENERAL);
        yRight += entryHeight;

        addOption(Button.builder(
                getPersistentModeText(),
                button -> {
                    editConfig.persistentMode = editConfig.persistentMode.next();
                    button.setMessage(getPersistentModeText());
                }
        ).tooltip(Tooltip.create(Component.translatable("config.ji_afkcinematic.tooltip.persistent_cinematics")))
         .bounds(col2X, yRight, widgetWidth, 20).build(), Category.AFK);
        yRight += entryHeight;


        addOption(Button.builder(
                getChatVisibilityText(),
                button -> {
                    editConfig.chatVisibility = editConfig.chatVisibility.next();
                    button.setMessage(getChatVisibilityText());
                }
        ).tooltip(Tooltip.create(Component.translatable("config.ji_afkcinematic.tooltip.chat_visibility")))
         .bounds(col2X, yRight, widgetWidth, 20).build(), Category.GENERAL);
        yRight += entryHeight;

        addOption(Button.builder(
                Component.translatable("config.ji_afkcinematic.camera_rotation")
                        .append(": ").append(getOnOffText(editConfig.cameraRotationEnabled)),
                button -> {
                    editConfig.cameraRotationEnabled = !editConfig.cameraRotationEnabled;
                    button.setMessage(Component.translatable("config.ji_afkcinematic.camera_rotation")
                            .append(": ").append(getOnOffText(editConfig.cameraRotationEnabled)));
                }
        ).tooltip(Tooltip.create(Component.translatable("config.ji_afkcinematic.tooltip.camera_rotation")))
         .bounds(col2X, yRight, widgetWidth, 20).build(), Category.CAMERA);
        yRight += entryHeight;

        addOption(Button.builder(
                Component.translatable("config.ji_afkcinematic.letterbox").append(": ").append(getOnOffText(editConfig.enableLetterbox)),
                button -> {
                    editConfig.enableLetterbox = !editConfig.enableLetterbox;
                    button.setMessage(Component.translatable("config.ji_afkcinematic.letterbox").append(": ").append(getOnOffText(editConfig.enableLetterbox)));
                }
        ).tooltip(Tooltip.create(Component.translatable("config.ji_afkcinematic.tooltip.letterbox"))).bounds(col2X, yRight, widgetWidth, 20).build(), Category.CAMERA);
        yRight += entryHeight;

        addOption(Button.builder(
                getSleepLetterboxText(),
                button -> {
                    editConfig.sleepLetterboxMode = editConfig.sleepLetterboxMode.next();
                    button.setMessage(getSleepLetterboxText());
                }
        ).tooltip(Tooltip.create(Component.translatable("config.ji_afkcinematic.tooltip.sleep_letterbox")))
                .bounds(col2X, yRight, widgetWidth, 20).build(), Category.SLEEP);
        yRight += entryHeight;

        addOption(Button.builder(
                Component.translatable("config.ji_afkcinematic.music").append(": ").append(getOnOffText(editConfig.enableMusic)),
                button -> {
                    editConfig.enableMusic = !editConfig.enableMusic;
                    button.setMessage(Component.translatable("config.ji_afkcinematic.music").append(": ").append(getOnOffText(editConfig.enableMusic)));
                }
        ).tooltip(Tooltip.create(Component.translatable("config.ji_afkcinematic.tooltip.music"))).bounds(col2X, yRight, widgetWidth, 20).build(), Category.MUSIC);
        yRight += entryHeight;

        addOption(Button.builder(
                Component.translatable("config.ji_afkcinematic.extended_music").append(": ").append(getOnOffText(editConfig.extendedMusic)),
                button -> {
                    editConfig.extendedMusic = !editConfig.extendedMusic;
                    button.setMessage(Component.translatable("config.ji_afkcinematic.extended_music").append(": ").append(getOnOffText(editConfig.extendedMusic)));
                }
        ).tooltip(Tooltip.create(Component.translatable("config.ji_afkcinematic.tooltip.extended_music"))).bounds(col2X, yRight, widgetWidth, 20).build(), Category.MUSIC);
        yRight += entryHeight;

        addOption(Button.builder(
                getMusicModeText(),
                button -> {
                    editConfig.musicMode = editConfig.musicMode.next();
                    button.setMessage(getMusicModeText());
                    updateMusicFolderVisibility();
                }
        ).tooltip(Tooltip.create(Component.translatable("config.ji_afkcinematic.tooltip.music_mode")))
         .bounds(col1X, yLeft, widgetWidth, 20).build(), Category.MUSIC);
        yLeft += entryHeight;

        addOption(Button.builder(
                Component.translatable("config.ji_afkcinematic.fishing_cinematic").append(": ")
                        .append(getOnOffText(editConfig.fishingCinematicEnabled)),
                button -> {
                    editConfig.fishingCinematicEnabled = !editConfig.fishingCinematicEnabled;
                    button.setMessage(Component.translatable("config.ji_afkcinematic.fishing_cinematic")
                            .append(": ").append(getOnOffText(editConfig.fishingCinematicEnabled)));
                    layoutCategoryWidgets();
                    configureScrolling();
                }
        ).tooltip(Tooltip.create(Component.translatable("config.ji_afkcinematic.tooltip.fishing_cinematic")
                ))
                .bounds(col1X, yLeft, widgetWidth, 20).build(), Category.SLEEP);
        yLeft += entryHeight;

        fishingThresholdSlider = addOption(new AbstractSliderButton(
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
        }, Category.SLEEP);
        yLeft += entryHeight;

        int centerStartY = Math.max(yLeft, yRight) + 5;

        manageMusicButton=Button.builder(Component.translatable("config.ji_afkcinematic.manage_music"),
                button->com.ji.afkcinematic.ScreenHelper.setScreen(Minecraft.getInstance(),new MusicManagementScreen(this,editConfig)))
                .tooltip(Tooltip.create(Component.translatable("config.ji_afkcinematic.tooltip.manage_music")))
                .bounds(col1X,centerStartY,widgetWidth,20).build();
        addOption(manageMusicButton,Category.MUSIC);
        refreshMusicButton = Button.builder(
                Component.translatable("config.ji_afkcinematic.refresh_music"),
                button -> {
                    if (!java.util.Objects.equals(editConfig.customMusicDirectory, ConfigManager.getConfig().customMusicDirectory)) {
                        button.setMessage(Component.translatable("config.ji_afkcinematic.music_save_folder"));
                        button.setTooltip(Tooltip.create(Component.translatable("config.ji_afkcinematic.tooltip.music_save_folder")));
                        return;
                    }
                    button.active = false;
                    busyWidgets.add(button);
                    long generation = uiWork.generation();
                    ModConfig workingCopy = editConfig;
                    LocalMusicPackManager.rebuildAndReloadAsync(() -> acceptJobResult(generation, workingCopy), trackCount -> {
                        if (!acceptJobResult(generation, workingCopy)) return;
                        busyWidgets.remove(button);
                        button.setMessage(trackCount < 0
                                ? Component.translatable("config.ji_afkcinematic.refresh_music_failed")
                                : Component.translatable("config.ji_afkcinematic.music_loaded", trackCount));
                        button.setTooltip(Tooltip.create(Component.translatable(trackCount < 0
                                ? "config.ji_afkcinematic.tooltip.music_load_failed" : "config.ji_afkcinematic.tooltip.music_loaded",
                                LocalMusicPackManager.getLastSkippedCount())));
                        applyScrolling();
                    });
                }
        ).tooltip(Tooltip.create(Component.translatable("config.ji_afkcinematic.tooltip.refresh_music")))
         .bounds(col2X, centerStartY, widgetWidth, 20).build();
        addOption(refreshMusicButton, Category.MUSIC);
        centerStartY += entryHeight+5;

        // Menu shortcut. Clicking it starts the normal two-key rebind flow.
        menuKeyButton = Button.builder(
            getMenuKeysText(),
            button -> startMenuRebind()
        ).tooltip(Tooltip.create(Component.translatable("config.ji_afkcinematic.tooltip.menu_keys")))
         .bounds(col1X, centerStartY, widgetWidth, 20).build();
        addOption(menuKeyButton, Category.CONTROLS);

        // Quick-toggle shortcut.
        toggleKeyButton = Button.builder(
            getToggleKeysText(),
            button -> startToggleRebind()
        ).tooltip(Tooltip.create(Component.translatable("config.ji_afkcinematic.tooltip.toggle_keys")))
         .bounds(col2X, centerStartY, widgetWidth, 20).build();
        addOption(toggleKeyButton, Category.CONTROLS);

        centerStartY += entryHeight;

        immediateKeyButton = Button.builder(
            getImmediateKeysText(), button -> startImmediateRebind()
        ).tooltip(Tooltip.create(Component.translatable("config.ji_afkcinematic.tooltip.immediate_keys")))
         .bounds(col1X, centerStartY, widgetWidth, 20).build();
        addOption(immediateKeyButton, Category.CONTROLS);

        modEnabledButton = Button.builder(
                Component.translatable("config.ji_afkcinematic.enabled").append(": ").append(getActiveDisabledText(editConfig.modEnabled)),
                button -> {
                    editConfig.modEnabled = !editConfig.modEnabled;
                    button.setMessage(Component.translatable("config.ji_afkcinematic.enabled").append(": ").append(getActiveDisabledText(editConfig.modEnabled)));
                }
        ).tooltip(Tooltip.create(Component.translatable("config.ji_afkcinematic.tooltip.enabled"))).bounds(col2X, centerStartY, widgetWidth, 20).build();
        addOption(modEnabledButton, Category.GENERAL);

        this.reportButton = Button.builder(
                Component.translatable("config.ji_afkcinematic.support_open"),
                ignored -> { com.ji.afkcinematic.ScreenHelper.setScreen(Minecraft.getInstance(), new SupportScreen(this)); }
        ).tooltip(Tooltip.create(Component.translatable("config.ji_afkcinematic.tooltip.feedback")))
         .bounds(col1X, getContentTop(), widgetWidth, 20).build();
        addOption(this.reportButton, Category.SUPPORT);
        captureScrollableWidgets();
        layoutCategoryWidgets();

        int bottomY = this.height - 35;
        int footerWidth = Math.min(100, (width - 32 - 10) / 3);
        int footerLeft = (width - (footerWidth * 3 + 10)) / 2;

        resetButton = this.addRenderableWidget(Button.builder(
                Component.translatable("config.ji_afkcinematic.reset_defaults"),
                button -> {
                    if (rebindState != RebindState.IDLE) cancelRebind();
                    this.editConfig = new ModConfig();
                    this.editConfig.recalculate();
                    this.rebuildWidgets();
                }
        ).bounds(footerLeft, bottomY, footerWidth, 20).build());

        saveButton = this.addRenderableWidget(Button.builder(
                Component.translatable(width < 380 ? "config.ji_afkcinematic.save_short" : "config.ji_afkcinematic.save"),
                button -> {
                    ModConfig current = ConfigManager.getConfig();
                    if (!com.ji.afkcinematic.input.RuntimeShortcuts.conflicts(editConfig).isEmpty()
                            && !com.ji.afkcinematic.input.RuntimeShortcuts.bindings(editConfig).equals(
                                    com.ji.afkcinematic.input.RuntimeShortcuts.bindings(current))) {
                        button.setMessage(Component.translatable("config.ji_afkcinematic.shortcut_conflict"));
                        button.setTooltip(Tooltip.create(Component.translatable("config.ji_afkcinematic.tooltip.shortcut_conflict")));
                        return;
                    }
                    boolean musicDirectoryChanged = !java.util.Objects.equals(current.customMusicDirectory, editConfig.customMusicDirectory);
                    boolean musicSettingsChanged = current.musicMode != editConfig.musicMode
                            || current.enableMusic != editConfig.enableMusic
                            || current.extendedMusic != editConfig.extendedMusic
                            || !java.util.Objects.equals(current.customMusicDirectory, editConfig.customMusicDirectory);
                    editConfig.recalculate();
                    if (saving || ConfigManager.isLoading()) return;
                    saving = true;
                    button.active = false; button.setMessage(Component.translatable("config.ji_afkcinematic.saving"));
                    ConfigManager.trySetConfigAsync(editConfig).thenAccept(success -> {
                        saving = false;
                        if (com.ji.afkcinematic.ScreenHelper.getCurrentScreen(Minecraft.getInstance()) != this) return;
                        if (!success) {
                            rebuildWidgets();
                            saveButton.setMessage(Component.translatable("config.ji_afkcinematic.save_failed"));
                            saveButton.setTooltip(Tooltip.create(Component.translatable("config.ji_afkcinematic.tooltip.save_failed")));
                            return;
                        }
                        if (musicDirectoryChanged) {
                            saving = true;
                            button.active = false;
                            LocalMusicPackManager.rebuildAndReloadAsync(trackCount -> {
                                if (trackCount < 0) {
                                    // Folder selection, durable settings and the generated pack form one user action.
                                    ConfigManager.trySetConfigAsync(current).thenAccept(restored -> {
                                        saving = false;
                                        if (com.ji.afkcinematic.ScreenHelper.getCurrentScreen(Minecraft.getInstance()) != this) return;
                                        rebuildWidgets();
                                        saveButton.setMessage(Component.translatable("config.ji_afkcinematic.refresh_music_failed"));
                                        saveButton.setTooltip(Tooltip.create(Component.translatable(restored
                                                ? "config.ji_afkcinematic.tooltip.music_folder_rolled_back"
                                                : "config.ji_afkcinematic.tooltip.save_failed")));
                                    });
                                } else {
                                    saving = false;
                                    com.ji.afkcinematic.music.CinematicMusicManager.onSettingsChanged();
                                    if (com.ji.afkcinematic.ScreenHelper.getCurrentScreen(Minecraft.getInstance()) == this) this.onClose();
                                }
                            });
                        } else {
                            if (musicSettingsChanged) com.ji.afkcinematic.music.CinematicMusicManager.onSettingsChanged();
                            this.onClose();
                        }
                    });
                }
        ).bounds(footerLeft + footerWidth + 5, bottomY, footerWidth, 20).build());

        Button cancelButton = this.addRenderableWidget(Button.builder(
                Component.translatable("config.ji_afkcinematic.cancel"),
                button -> this.onClose()
        ).bounds(footerLeft + (footerWidth + 5) * 2, bottomY, footerWidth, 20).build());
        this.discordCornerButton = this.addRenderableWidget(Button.builder(Component.literal("§e⚠"), net.minecraft.client.gui.screens.ConfirmLinkScreen.confirmLink(this, java.net.URI.create("https://discord.gg/sE27D5SNaq")))
                .tooltip(Tooltip.create(Component.translatable("config.ji_afkcinematic.tooltip.report")))
                .bounds(this.width-35,this.height-35,30,30).build());
        this.websiteCornerButton = this.addRenderableWidget(Button.builder(Component.literal(""),
                net.minecraft.client.gui.screens.ConfirmLinkScreen.confirmLink(this,java.net.URI.create("https://ji-afk-cinematic.ji-mods.com")))
                .tooltip(Tooltip.create(Component.translatable("config.ji_afkcinematic.tooltip.website")))
                .bounds(this.width-70,this.height-35,30,30).build());
        saveButton.active = !ConfigManager.isWriteProtected();
        discordCornerButton.visible = websiteCornerButton.visible = width >= 460;
        configureScrolling();
    }

    private <T extends AbstractWidget> T addOption(T option, Category category) {
        widgetCategories.put(option, category);
        return this.addRenderableWidget(option);
    }

    private int contentWidth() { return Math.max(140,Math.min(optionColumns()==2?280:220,width-40)); }
    private int optionColumns() { return width<320?1:2; }
    public String getSelectedCategoryKey() { return selectedCategory.key; }
    /** Retained for internal links: scroll to the group without hiding any options. */
    public void selectCategory(String key) {
        for(Category category:Category.values())if(category.key.equals(key)) {
            selectedCategory=category;
            if(rebindState!=RebindState.IDLE)cancelRebind();
            for(AbstractWidget option:scrollableWidgets)if(widgetCategories.get(option)==category && isAvailableOption(option)) {
                contentScroll=Math.max(0,Math.min(maxContentScroll,scrollableBaseY.get(option)-getContentTop()));applyScrolling();return;
            }
            return;
        }
        throw new IllegalArgumentException("Unknown config group: "+key);
    }

    private boolean isAvailableOption(AbstractWidget option) {
        boolean folderControl = fileControl(option);
        return (!folderControl || editConfig.musicMode.includesCustom())
                && (option != fishingThresholdSlider || editConfig.fishingCinematicEnabled);
    }

    private boolean fileControl(AbstractWidget option) { return option==manageMusicButton||option==refreshMusicButton; }
    private boolean shortcutControl(AbstractWidget option) { return widgetCategories.get(option)==Category.CONTROLS||option==modEnabledButton; }
    private void placeOption(AbstractWidget option,int column,int y,int columns) {
        int gap=10,optionWidth=(contentWidth()-gap*(columns-1))/columns;
        option.setX((width-contentWidth())/2+column*(optionWidth+gap));option.setWidth(optionWidth);scrollableBaseY.put(option,y);
    }
    private void layoutCategoryWidgets() {
        int columns=optionColumns(),top=getContentTop()+4,leftY=top,rightY=top;
        // Preserve v2.3.2's two independent columns and their original insertion order.
        for(int legacy=0;legacy<2;legacy++) {
            int y=columns==1?leftY:top;
            for(AbstractWidget option:scrollableWidgets) {
                if(!isAvailableOption(option)||option==reportButton||fileControl(option)||shortcutControl(option))continue;
                if(Boolean.TRUE.equals(legacyLeftColumn.get(option))!=(legacy==0))continue;
                placeOption(option,columns==1?0:legacy,y,columns);y+=26;
            }
            if(legacy==0||columns==1)leftY=y;else rightY=y;
        }
        int y=Math.max(leftY,rightY)+5,index=0;
        for(AbstractWidget option:scrollableWidgets)if(isAvailableOption(option)&&fileControl(option)) {
            placeOption(option,index%columns,y+index/columns*26,columns);index++;
        }
        if(index>0)y+=(index+columns-1)/columns*26;
        y+=5;index=0;
        for(AbstractWidget option:scrollableWidgets)if(isAvailableOption(option)&&shortcutControl(option)) {
            placeOption(option,index%columns,y+index/columns*26,columns);index++;
        }
        y+=(index+columns-1)/columns*26;
        if(reportButton!=null) {reportButton.setX((width-contentWidth())/2);reportButton.setWidth(contentWidth());scrollableBaseY.put(reportButton,y+4);}
    }

    private int getViewportBottom() {
        return this.height - (ConfigManager.isWriteProtected() ? 65 : 45);
    }

    private boolean acceptJobResult(long generation, ModConfig workingCopy) {
        return uiWork.accepts(generation) && workingCopy == editConfig && com.ji.afkcinematic.ScreenHelper.getCurrentScreen(Minecraft.getInstance()) == this;
    }

    @Override
    public void removed() {
        LocalMusicPackManager.cancelScreenJobs();
        uiWork.begin();
        busyWidgets.clear();
        if (rebindState != RebindState.IDLE) cancelRebind();
        super.removed();
    }

    private void captureScrollableWidgets() {
        for (Object child : this.children()) {
            if (child instanceof AbstractWidget widget && widgetCategories.containsKey(widget)) {
                scrollableWidgets.add(widget);
                scrollableBaseY.put(widget, widget.getY());
                legacyLeftColumn.put(widget,widget.getX()<width/2);
            }
        }
    }

    private void configureScrolling() {
        int viewportBottom = getViewportBottom();
        int contentBottom = getContentTop();
        for (AbstractWidget widget : scrollableWidgets) {
            if (isAvailableOption(widget)) {
                contentBottom = Math.max(contentBottom, scrollableBaseY.get(widget) + widget.getHeight());
            }
        }
        maxContentScroll = Math.max(0, contentBottom - viewportBottom + 4);
        contentScroll = Math.max(0, Math.min(contentScroll, maxContentScroll));
        applyScrolling();
    }

    private void applyScrolling() {
        int viewportBottom = getViewportBottom();
        for (AbstractWidget widget : scrollableWidgets) {
            int y = scrollableBaseY.get(widget) - contentScroll;
            widget.setY(y);
            boolean inside = y >= getContentTop() && y + widget.getHeight() <= viewportBottom;
            widget.visible = inside && isAvailableOption(widget);
            widget.active = widget.visible && !busyWidgets.contains(widget);
        }
    }

    private int getContentTop() { return height<360?58:92; }

    private void updateMusicFolderVisibility() {
        layoutCategoryWidgets();
        configureScrolling();
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
        ChatFormatting color = editConfig.sleepLetterboxMode == SleepLetterboxMode.DISABLED ? ChatFormatting.RED : ChatFormatting.GREEN;
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
        return Component.translatable("config.ji_afkcinematic.chat_visibility")
                .append(": ").append(getOnOffText(editConfig.chatVisibility == CinematicChatVisibility.VISIBLE));
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
        if (discordCornerButton != null) discordCornerButton.setMessage(Component.literal(com.ji.afkcinematic.client.SupportCornerIcon.at(System.currentTimeMillis())));
        if (websiteCornerButton != null) websiteCornerButton.setMessage(Component.literal(com.ji.afkcinematic.client.ReleaseLabel.websiteIcon(System.currentTimeMillis())));
        super.extractRenderState(context, mouseX, mouseY, delta);
        ScreenBrand.drawWebsiteGlobe(context,websiteCornerButton);

        if (maxContentScroll > 0) {
            int top = getContentTop();
            int bottom = getViewportBottom();
            int trackX = (this.width + contentWidth()) / 2 + 10;
            int trackHeight = bottom - top;
            int thumbHeight = Math.max(18, trackHeight * trackHeight / (trackHeight + maxContentScroll));
            int thumbY = top + (trackHeight - thumbHeight) * contentScroll / maxContentScroll;
            context.fill(trackX, top, trackX + 3, bottom, 0x55333333);
            context.fill(trackX, thumbY, trackX + 3, thumbY + thumbHeight, 0xFFAAAAAA);
        }


        int titleY = this.height < 360 ? 23 : 61;
        ScreenBrand.draw(context,this.font,this.width,titleY);
        if (ConfigManager.isWriteProtected()) {
            context.centeredText(this.font, Component.translatable("config.ji_afkcinematic.protected_notice"), this.width / 2, this.height - 57, 0xFFFFAA55);
        }
        context.text(this.font, Component.translatable("config.ji_afkcinematic.footer"), 8, this.height - 12, 0xFFD8A7FF);
    }

    /**
     * Hardening v2.2.2: contador en client tick para restaurar el label tras
     * UNSUPPORTED_KEY_REJECTED. Llamado por Screen cada frame.
     */
    @Override
    public void tick() {
        updateFocusTooltip();
        if (awaitingConfig && !ConfigManager.isLoading()) { awaitingConfig = false; cloneConfig(); rebuildWidgets(); }
        super.tick();
        if (getFocused() instanceof AbstractWidget focused && !focused.visible) setFocused(null);
        if (saving || ConfigManager.isLoading()) {
            for (Object child : children()) if (child instanceof AbstractWidget widget) widget.active = false;
            if (saveButton != null) saveButton.setMessage(Component.translatable("config.ji_afkcinematic.saving"));
            return;
        }
        if (saveButton != null) {
            saveButton.active = !ConfigManager.isWriteProtected();
            if (!saveButton.active) {
                saveButton.setMessage(Component.translatable("config.ji_afkcinematic.save_protected"));
                saveButton.setTooltip(Tooltip.create(Component.translatable("config.ji_afkcinematic.tooltip.protected_notice")));
            }
        }
        if (resetConfirmationTicks > 0 && --resetConfirmationTicks == 0 && resetButton != null) {
            resetButton.setMessage(Component.translatable("config.ji_afkcinematic.reset_defaults"));
        }
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
    public boolean keyReleased(KeyEvent event) {
        if (rebindState != RebindState.IDLE) {
            int[] keys = new int[2];
            int result = com.ji.afkcinematic.input.KeySequenceTracker.processRebindRelease(event.key(), keys);
            if (result == -1) cancelRebind();
            else if (result == 2) {
                if (rebindState == RebindState.MENU_WAITING_SECOND) {
                    editConfig.menuKey1 = keys[0]; editConfig.menuKey2 = keys[1];
                } else if (rebindState == RebindState.TOGGLE_WAITING_SECOND) {
                    editConfig.toggleKey1 = keys[0]; editConfig.toggleKey2 = keys[1];
                } else if (rebindState == RebindState.IMMEDIATE_WAITING_SECOND) {
                    editConfig.immediateKey1 = keys[0]; editConfig.immediateKey2 = keys[1];
                }
                rebindState = RebindState.IDLE;
                refreshKeyButtonLabels();
            }
            return true;
        }
        return super.keyReleased(event);
    }

    private void updateFocusTooltip() {
        if (focusSlider == null) return;
        String reason = com.ji.afkcinematic.cinematic.FocusStatus.reason();
        var tip = Component.translatable("config.ji_afkcinematic.tooltip.focus_level");
        if (!reason.equals("ready")) tip.append("\n").append(Component.translatable("config.ji_afkcinematic.focus_status." + reason));
        tip.append("\n\n").append(Component.literal("[Beta]").withStyle(ChatFormatting.YELLOW));
        focusSlider.setTooltip(Tooltip.create(tip));
    }

    @Override
    public void onClose() {
        if (saving || ConfigManager.isLoading()) return; // Save is an explicit commit; close only after its durable result.
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
        if (k1 == k2) return getKeyName(k1);
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
