package com.ji.afkcinematic.cinematic;

/** Pure timing helpers that keep camera velocity independent from shot duration. */
public final class ShotMotion {
    private ShotMotion() {}

    public static float elapsedSeconds(float frameProgress, int durationSeconds) {
        float safeProgress = Float.isFinite(frameProgress)
                ? Math.max(0.0f, Math.min(1.0f, frameProgress)) : 0.0f;
        return safeProgress * Math.max(0, durationSeconds);
    }

    public static float phase(float elapsedSeconds, float speedMultiplier, float travelSeconds) {
        return Math.min(1.0f, travel(elapsedSeconds, speedMultiplier, travelSeconds));
    }

    /** Unbounded travel used by continuous orbital motion after framing settles. */
    public static float travel(float elapsedSeconds, float speedMultiplier, float travelSeconds) {
        if (!Float.isFinite(elapsedSeconds) || !Float.isFinite(speedMultiplier)
                || !Float.isFinite(travelSeconds) || travelSeconds <= 0.0f) {
            return 0.0f;
        }
        return Math.max(0.0f, elapsedSeconds * Math.max(0.0f, speedMultiplier) / travelSeconds);
    }
}
