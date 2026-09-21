package com.ji.afkcinematic.config;

import com.ji.afkcinematic.input.KeySequenceTracker;
import com.ji.afkcinematic.input.PortableKeyBinding;

public class ModConfig {
    // Schema version for forward migration. Bump when a breaking field change happens;
    // add migration logic in ConfigManager.loadConfig().
    public static final int CURRENT_CONFIG_VERSION = 14;
    public static final int UNLIMITED_CYCLES = -1;
    public int configVersion = CURRENT_CONFIG_VERSION;

    // Cinematic timing
    public int shotDurationSeconds = 10;
    public int afkThresholdSeconds = 30;
    public int maxCycles = 3;

    // Camera behavior
    public float cameraSpeed = 0.5f;
    // Legacy runtime fields retained for source compatibility. These settings are no
    // longer serialized or user-configurable; recalculate() enforces their policy.
    @Deprecated public transient boolean useEasing = false;
    @Deprecated public transient float easingIntensity = 0.0f;
    // Fifteen curated shots are selected per cycle from two fifteen-shot pools.
    public int characterShotPercentage = 30;
    // Controls which interactions may interrupt an active cinematic.
    public PersistentCinematicMode persistentMode = PersistentCinematicMode.NORMAL;
    public boolean cameraRotationEnabled = false;

    // Safety
    public DamageAction damageAction = DamageAction.CANCEL_CINEMATIC;
    @Deprecated public transient boolean cancelOnFallDamage = false;
    @Deprecated public transient boolean cancelOnFire = false;
    @Deprecated public transient float lowHealthThreshold = 0.0f;

    // Visual
    public boolean extendedMusic = true;
    public boolean modEnabled = true;
    public boolean enableLetterbox = true;
    public SleepLetterboxMode sleepLetterboxMode = SleepLetterboxMode.MODERATE;
    public boolean fishingCinematicEnabled = true;
    public int fishingCinematicThresholdSeconds = 10;
    public CinematicChatVisibility chatVisibility = CinematicChatVisibility.VISIBLE;

    // Audio
    public boolean enableMusic = true;
    public MusicMode musicMode = MusicMode.VANILLA;
    public String customMusicDirectory = "";
    @Deprecated public transient boolean thirdPartyMusic = false;

    // Keybind (GLFW key code, F7 = 296)
    public int menuKey1 = 296;
    public int menuKey2 = 72;
    public int toggleKey1 = 341;
    public int toggleKey2 = 72;
    public int immediateKey1 = 296;
    public int immediateKey2 = 73;

    // Stable vanilla identities survive the GLFW/SDL boundary between game versions.
    public String menuKey1Name = "key.keyboard.f7";
    public String menuKey2Name = "key.keyboard.h";
    public String toggleKey1Name = "key.keyboard.left.control";
    public String toggleKey2Name = "key.keyboard.h";
    public String immediateKey1Name = "key.keyboard.f7";
    public String immediateKey2Name = "key.keyboard.i";
    public float cinematicMusicVolume = 0.5f;

    // Derived helpers (not serialized, transient)
    public transient int shotDurationTicks = 200;
    public transient int afkThresholdTicks = 1200;
    public transient int fishingCinematicThresholdTicks = 200;

    public void recalculate() {
        // Clamp values to valid ranges
        shotDurationSeconds = clamp(shotDurationSeconds, 5, 60);
        afkThresholdSeconds = clamp(afkThresholdSeconds, 10, 600);
        if (maxCycles != UNLIMITED_CYCLES) maxCycles = clamp(maxCycles, 1, 20);
        cameraSpeed = clampFloat(cameraSpeed, 0.1f, 3.0f);
        useEasing = false;
        easingIntensity = 0.0f;
        if (persistentMode == null) persistentMode = PersistentCinematicMode.NORMAL;
        if (chatVisibility == null) chatVisibility = CinematicChatVisibility.VISIBLE;
        if (musicMode == null) musicMode = MusicMode.VANILLA;
        if (sleepLetterboxMode == null) sleepLetterboxMode = SleepLetterboxMode.MODERATE;
        if (customMusicDirectory == null) customMusicDirectory = "";
        fishingCinematicThresholdSeconds = clamp(fishingCinematicThresholdSeconds, 0, 120);
        characterShotPercentage = Math.round(clamp(characterShotPercentage, 0, 100) / 10.0f) * 10;
        cancelOnFallDamage = false;
        cancelOnFire = false;
        lowHealthThreshold = 0.0f;
        cinematicMusicVolume = clampFloat(cinematicMusicVolume, 0.0f, 1.0f);

        // Keybinds: GLFW_KEY_UNKNOWN (-1) is the sentinel for "none"; clamp anything wild
        // into a sane range covering all GLFW key + button codes.
        menuKey1 = clamp(menuKey1, -1, 65535);
        menuKey2 = clamp(menuKey2, -1, 65535);
        toggleKey1 = clamp(toggleKey1, -1, 65535);
        toggleKey2 = clamp(toggleKey2, -1, 65535);
        immediateKey1 = clamp(immediateKey1, -1, 65535);
        immediateKey2 = clamp(immediateKey2, -1, 65535);
        // If the menu and toggle sequences are identical the tracker can't tell them
        // apart; fall back to the built-in defaults and warn so the user notices.
        if (menuKey1 == toggleKey1 && menuKey2 == toggleKey2
                && !isDisabledShortcut(menuKey1, menuKey2)) {
            com.ji.afkcinematic.JiAFKCinematic.LOGGER.warn(
                "Menu and toggle keybinds collide ({}, {}); resetting to defaults",
                menuKey1, menuKey2);
            menuKey1 = 296; menuKey2 = 72;
            toggleKey1 = 341; toggleKey2 = 72;
        }

        shotDurationTicks = shotDurationSeconds * 20;
        afkThresholdTicks = afkThresholdSeconds * 20;
        fishingCinematicThresholdTicks = fishingCinematicThresholdSeconds * 20;

        // -1/-1 representa un atajo deshabilitado y debe sobrevivir al guardado.
        // Cualquier otro par debe contener dos keycodes GLFW representables.
        if (!isDisabledShortcut(menuKey1, menuKey2)
                && (!KeySequenceTracker.isBindableKeyCode(menuKey1)
                    || !KeySequenceTracker.isBindableKeyCode(menuKey2))) {
            com.ji.afkcinematic.JiAFKCinematic.LOGGER.warn(
                "Invalid menu shortcut ({}, {}); resetting to F7 + H", menuKey1, menuKey2);
            menuKey1 = 296;
            menuKey2 = 72;
        }
        if (!isDisabledShortcut(toggleKey1, toggleKey2)
                && (!KeySequenceTracker.isBindableKeyCode(toggleKey1)
                    || !KeySequenceTracker.isBindableKeyCode(toggleKey2))) {
            com.ji.afkcinematic.JiAFKCinematic.LOGGER.warn(
                "Invalid toggle shortcut ({}, {}); resetting to Ctrl + H", toggleKey1, toggleKey2);
            toggleKey1 = 341;
            toggleKey2 = 72;
        }
        if (!isDisabledShortcut(immediateKey1, immediateKey2)
                && (!KeySequenceTracker.isBindableKeyCode(immediateKey1)
                    || !KeySequenceTracker.isBindableKeyCode(immediateKey2))) {
            com.ji.afkcinematic.JiAFKCinematic.LOGGER.warn(
                "Invalid immediate shortcut ({}, {}); resetting to F7 + I", immediateKey1, immediateKey2);
            immediateKey1 = 296;
            immediateKey2 = 73;
        }
    }


    /** Resolves stable names into the native codes used by this game version. */
    public void applyPortableKeyNames() {
        menuKey1 = PortableKeyBinding.codeOf(menuKey1Name, menuKey1);
        menuKey2 = PortableKeyBinding.codeOf(menuKey2Name, menuKey2);
        toggleKey1 = PortableKeyBinding.codeOf(toggleKey1Name, toggleKey1);
        toggleKey2 = PortableKeyBinding.codeOf(toggleKey2Name, toggleKey2);
        immediateKey1 = PortableKeyBinding.codeOf(immediateKey1Name, immediateKey1);
        immediateKey2 = PortableKeyBinding.codeOf(immediateKey2Name, immediateKey2);
    }

    /** Captures the current native bindings as stable names before serialization. */
    public void syncPortableKeyNames() {
        menuKey1Name = stableName(menuKey1, menuKey1Name);
        menuKey2Name = stableName(menuKey2, menuKey2Name);
        toggleKey1Name = stableName(toggleKey1, toggleKey1Name);
        toggleKey2Name = stableName(toggleKey2, toggleKey2Name);
        immediateKey1Name = stableName(immediateKey1, immediateKey1Name);
        immediateKey2Name = stableName(immediateKey2, immediateKey2Name);
    }

    private static String stableName(int nativeCode, String fallback) {
        String name = PortableKeyBinding.nameOf(nativeCode);
        return name == null ? fallback : name;
    }
    public boolean isUnlimitedCycles() {
        return maxCycles == UNLIMITED_CYCLES;
    }

    private static boolean isDisabledShortcut(int firstKey, int secondKey) {
        return firstKey == -1 && secondKey == -1;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static float clampFloat(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
