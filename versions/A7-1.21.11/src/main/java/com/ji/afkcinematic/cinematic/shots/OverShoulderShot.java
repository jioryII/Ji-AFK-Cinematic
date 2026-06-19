package com.ji.afkcinematic.cinematic.shots;

import com.ji.afkcinematic.cinematic.AbstractCameraShot;

import com.ji.afkcinematic.cinematic.CameraShot;
import com.ji.afkcinematic.cinematic.ShotRandomizer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.Vec3d;

/**
 * Over-the-shoulder shot — close behind/beside the player looking forward.
 * Slow drift with slight distance increase.
 */
public class OverShoulderShot extends AbstractCameraShot {
    private float startAngle;

    @Override
    public void start() {
        startAngle = ShotRandomizer.getRandomStartAngle();
    }

    @Override
    public Vec3d updatePosition(float progress, float speedMultiplier, float tickDelta) {
        if (!isPlayerAvailable()) return Vec3d.ZERO;

        // Constant slow drift, scaled by speed multiplier
        float scaledProgress = progress * speedMultiplier;
        float currentAngle = startAngle + (scaledProgress * 5f);
        float currentDist = 3f + (scaledProgress * 1.5f);

        Vec3d offset = getCircularOffset(currentAngle, currentDist);

        return getPlayerPos(tickDelta).add(offset.x, 2.5f, offset.z);
    }

    @Override
    public float updatePitch(float progress, float speedMultiplier, float tickDelta) {
        return 5f;
    }

    @Override
    public float updateYaw(float progress, float speedMultiplier, float tickDelta) {
        return startAngle - 45f;
    }
}
