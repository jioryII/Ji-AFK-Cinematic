package com.ji.afkcinematic.config;

public class ModConfig {
    // Cinematic timing
    public int shotDurationSeconds = 10;
    public int afkThresholdSeconds = 30;
    public int maxCycles = 3;

    // Camera behavior
    public float cameraSpeed = 0.5f;

    // Safety
    public DamageAction damageAction = DamageAction.CANCEL_CINEMATIC;

    // Visual
    public boolean extendedMusic = true;
    public boolean modEnabled = true;
    public boolean enableLetterbox = true;

    // Audio
    public boolean enableMusic = true;

    // Keybind (GLFW key code, F7 = 296)
    public int menuKey1 = 296;
    public int menuKey2 = 72;
    public int toggleKey1 = 341;
    public int toggleKey2 = 72;
    public float cinematicMusicVolume = 1.0f;

    // Derived helpers (not serialized, transient)
    public transient int shotDurationTicks = 200;
    public transient int afkThresholdTicks = 1200;

    public void recalculate() {
        // Clamp values to valid ranges
        shotDurationSeconds = clamp(shotDurationSeconds, 5, 60);
        afkThresholdSeconds = clamp(afkThresholdSeconds, 10, 600);
        maxCycles = clamp(maxCycles, 1, 20);
        cameraSpeed = clampFloat(cameraSpeed, 0.1f, 3.0f);
        cinematicMusicVolume = clampFloat(cinematicMusicVolume, 0.0f, 1.0f);

        shotDurationTicks = shotDurationSeconds * 20;
        afkThresholdTicks = afkThresholdSeconds * 20;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static float clampFloat(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
