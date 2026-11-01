package com.ji.afkcinematic.compat;

/** Optional, provider-scoped bridge; a foreign mixin's similarly named fields are never reset. */
public final class SmoothF5Compatibility {
    private static volatile boolean snapOnNextCameraUpdate;
    private static volatile boolean targetDetached, targetMirrored;
    private static final class Detection {
        static final boolean PRESENT = com.ji.afkcinematic.diagnostic.DiagnosticPlatform.mods().stream()
                .anyMatch(mod -> mod.id().equals("smooth_f5"));
    }
    private SmoothF5Compatibility() { }
    public static boolean present() { return Detection.PRESENT; }
    public static void prepareImmediateReturn(boolean detached, boolean mirrored) {
        if (!present()) return;
        targetDetached = detached; targetMirrored = mirrored; snapOnNextCameraUpdate = true;
    }
    public static void onCameraUpdate(Object camera) {
        if (!snapOnNextCameraUpdate) return;
        snapOnNextCameraUpdate = false;
        SmoothF5StateReset.apply(camera, targetDetached, targetMirrored);
    }
}
