package com.ji.afkcinematic.cinematic;

import net.minecraft.util.math.Vec3d;

/** Pure geometry for vanilla fishing-particle association and shot-side selection. */
public final class FishingApproach {
    private FishingApproach() {}

    public static boolean belongsToHook(Vec3d hook, Vec3d particle) {
        double horizontal = Math.hypot(particle.x - hook.x, particle.z - hook.z);
        double vertical = Math.abs(particle.y - hook.y);
        return horizontal >= 0.75 && horizontal <= 8.5 && vertical <= 3.5;
    }

    /** Returns -1 for left, 0 for center and 1 for right, opposite the incoming fish. */
    public static int preferredCameraSide(Vec3d player, Vec3d hook, Vec3d particle) {
        Vec3d forward = new Vec3d(hook.x - player.x, 0, hook.z - player.z);
        if (forward.lengthSquared() < 0.01) return 0;
        forward = forward.normalize();
        Vec3d lateralAxis = new Vec3d(-forward.z, 0, forward.x);
        double incomingSide = particle.subtract(hook).dotProduct(lateralAxis);
        if (Math.abs(incomingSide) < 0.35) return 0;
        return incomingSide > 0 ? -1 : 1;
    }
}
