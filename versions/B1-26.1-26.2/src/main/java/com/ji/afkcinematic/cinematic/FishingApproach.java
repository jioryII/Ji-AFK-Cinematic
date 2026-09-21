package com.ji.afkcinematic.cinematic;

import net.minecraft.world.phys.Vec3;

/** Pure geometry for vanilla fishing-particle association and shot-side selection. */
public final class FishingApproach {
    private FishingApproach() {}

    public static boolean belongsToHook(Vec3 hook, Vec3 particle) {
        double horizontal = Math.hypot(particle.x - hook.x, particle.z - hook.z);
        double vertical = Math.abs(particle.y - hook.y);
        return horizontal >= 0.75 && horizontal <= 8.5 && vertical <= 3.5;
    }

    /** Returns -1 for left, 0 for center and 1 for right, opposite the incoming fish. */
    public static int preferredCameraSide(Vec3 player, Vec3 hook, Vec3 particle) {
        Vec3 forward = new Vec3(hook.x - player.x, 0, hook.z - player.z);
        if (forward.lengthSqr() < 0.01) return 0;
        forward = forward.normalize();
        Vec3 lateralAxis = new Vec3(-forward.z, 0, forward.x);
        double incomingSide = particle.subtract(hook).dot(lateralAxis);
        if (Math.abs(incomingSide) < 0.35) return 0;
        return incomingSide > 0 ? -1 : 1;
    }
}
