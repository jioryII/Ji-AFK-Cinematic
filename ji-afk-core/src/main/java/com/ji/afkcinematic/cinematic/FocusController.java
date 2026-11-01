package com.ji.afkcinematic.cinematic;

/** Bounded optical focus state. No native world, renderer or clock dependencies. */
public final class FocusController {
    /** Paused for this release; retain optics and configuration for future work. */
    public static final boolean AVAILABLE = false;
    public record Frame(double distance, double sharpHalfWidth, double radius1080, double opacity) {
        public static final Frame OFF = new Frame(1, 1, 0, 0);
        public boolean enabled() { return radius1080 > 0 && opacity > 0; }
    }
    private Object world;
    private long take = Long.MIN_VALUE;
    private double distance, halfWidth, age;

    public Frame update(Object worldKey, long takeId, double desiredDistance, double desiredHalfWidth,
                        int level, double deltaSeconds) {
        if (worldKey == null || level <= 0 || !Double.isFinite(desiredDistance)
                || desiredDistance < .05 || !Double.isFinite(desiredHalfWidth) || desiredHalfWidth < 0) {
            reset(); return Frame.OFF;
        }
        double dt = Double.isFinite(deltaSeconds) ? Math.max(0, Math.min(.1, deltaSeconds)) : 0;
        boolean newSession = world != worldKey;
        boolean cut = newSession || take != takeId;
        if (newSession) age = 0;
        world = worldKey; take = takeId;
        if (cut) { distance = desiredDistance; halfWidth = desiredHalfWidth; }
        else {
            // Smooth in optical power (diopters), independent of frame rate.
            double blend = -Math.expm1(-dt / .18);
            distance = 1 / ((1 / distance) + ((1 / desiredDistance) - (1 / distance)) * blend);
            halfWidth += (desiredHalfWidth - halfWidth) * blend;
        }
        age += dt;
        double radius = switch (Math.min(4, level)) { case 1 -> .85; case 2 -> 1.5; case 3 -> 2.35; default -> 3.5; };
        // A collision or a pose change must not blur the intended subject while the lens catches up.
        double protectedWidth = Math.max(halfWidth, desiredHalfWidth + Math.abs(distance - desiredDistance));
        return new Frame(distance, Math.max(.04, protectedWidth), radius, Math.min(1, age / .22));
    }

    public void reset() { world = null; take = Long.MIN_VALUE; distance = halfWidth = age = 0; }
}
