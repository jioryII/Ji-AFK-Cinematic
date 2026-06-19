package com.ji.afkcinematic.cinematic;

import net.minecraft.util.math.MathHelper;

public class EasingFunctions {
    public static float easeInOutCubic(float t) {
        return t < 0.5f ? 4.0f * t * t * t : 1.0f - (float) Math.pow(-2.0f * t + 2.0f, 3.0f) / 2.0f;
    }

    public static float easeOutCubic(float t) {
        return 1.0f - (float) Math.pow(1.0f - t, 3.0f);
    }

    public static float easeInOutSine(float t) {
        return -(float)(Math.cos(Math.PI * t) - 1.0) / 2.0f;
    }

    public static float smoothstep(float t) {
        return t * t * (3.0f - 2.0f * t);
    }
    
    // Custom lerp smoothing
    public static float smoothLerp(float start, float end, float t, float delta) {
        float factor = 1.0f - (float) Math.pow(1.0f - t, delta);
        return MathHelper.lerp(factor, start, end);
    }
}
