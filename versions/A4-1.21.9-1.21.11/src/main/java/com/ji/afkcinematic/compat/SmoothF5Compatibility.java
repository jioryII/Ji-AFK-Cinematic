package com.ji.afkcinematic.compat;

import java.lang.reflect.Field;

/** Optional, reflection-only bridge: Smooth F5 is never a required dependency. */
public final class SmoothF5Compatibility {
    private static volatile boolean snapOnNextCameraUpdate;
    private static volatile boolean targetDetached;
    private static volatile boolean targetMirrored;

    private SmoothF5Compatibility() {}

    public static void prepareImmediateReturn(boolean detached, boolean mirrored) {
        targetDetached = detached;
        targetMirrored = mirrored;
        snapOnNextCameraUpdate = true;
    }

    public static void onCameraUpdate(Object camera) {
        if (!snapOnNextCameraUpdate) return;
        snapOnNextCameraUpdate = false;
        resetState(camera, 0);
    }

    private static void resetState(Object target, int depth) {
        if (target == null || depth > 2) return;
        for (Field field : target.getClass().getDeclaredFields()) {
            String name = field.getName();
            try {
                field.setAccessible(true);
                if (name.endsWith("wasDetached")) {
                    field.setBoolean(target, targetDetached);
                } else if (name.endsWith("wasMirrored")) {
                    field.setBoolean(target, targetMirrored);
                } else if (name.endsWith("isTransitioning") || name.endsWith("transDeltaReady")
                        || name.endsWith("shouldSnapNextTail")) {
                    field.setBoolean(target, false);
                } else if (name.endsWith("smoother") || name.equals("state")) {
                    resetState(field.get(target), depth + 1);
                }
            } catch (ReflectiveOperationException | RuntimeException ignored) {
                // Smooth F5 absent or changed: vanilla return remains untouched.
            }
        }
    }
}
