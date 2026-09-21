package com.ji.afkcinematic.cinematic;

import java.util.Random;

/**
 * Selects one subtle, fixed Dutch angle per shot. Most shots remain level.
 */
public final class CameraRotationProfile {
    private static final float[] ANGLES = {
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            -0.4f, 0.4f, -0.65f, 0.65f, -0.9f, 0.9f
    };

    private CameraRotationProfile() {}

    public static float[] plan(int shotCount, long seed) {
        if (shotCount < 0) throw new IllegalArgumentException("shotCount must be non-negative");
        Random random = new Random(seed ^ 0x4A4943414D455241L);
        float[] result = new float[shotCount];
        for (int i = 0; i < result.length; i++) {
            result[i] = ANGLES[random.nextInt(ANGLES.length)];
        }
        return result;
    }

    public static float apply(float angle, boolean enabled) {
        if (!enabled || !Float.isFinite(angle)) {
            return 0.0f;
        }
        return Math.max(-0.9f, Math.min(0.9f, angle));
    }
}
