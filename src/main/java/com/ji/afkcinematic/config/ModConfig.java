package com.ji.afkcinematic.config;

public class ModConfig {
    // Cinematic timing
    public int shotDurationSeconds = 10;
    public int afkThresholdSeconds = 60;
    public int maxCycles = 3;

    // Camera behavior
    public float cameraSpeed = 1.0f;

    // Safety
    public DamageAction damageAction = DamageAction.PAUSE_GAME;

    // Visual
    public boolean showPlayerName = true;
    public boolean enableLetterbox = true;

    // Audio
    public boolean enableMusic = true;

    // Keybind (GLFW key code, F7 = 296)
    public int configKeyCode = 296;

    // Derived helpers (not serialized, transient)
    public transient int shotDurationTicks = 200;
    public transient int afkThresholdTicks = 1200;

    public void recalculate() {
        // Clamp values to valid ranges
        shotDurationSeconds = clamp(shotDurationSeconds, 5, 60);
        afkThresholdSeconds = clamp(afkThresholdSeconds, 10, 600);
        maxCycles = clamp(maxCycles, 1, 20);
        cameraSpeed = clampFloat(cameraSpeed, 0.1f, 3.0f);

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
