package com.ji.afkcinematic.cinematic.shots;

import com.ji.afkcinematic.cinematic.AbstractCameraShot;


import com.ji.afkcinematic.cinematic.CameraShot;
import com.ji.afkcinematic.cinematic.ShotRandomizer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.Vec3d;

/**
 * Panoramic sweep — shows the environment around the player.
 * Distance: 7 blocks, Height: 4 blocks, Sweep: 25°
 */
public class PanoramaSweepShot extends AbstractCameraShot {
    private float startAngle;
    private boolean clockwise;

    private static final float DISTANCE = 7f;
    private static final float HEIGHT = 4f;
    private static final float SWEEP_DEGREES = 25f;

    @Override
    public void start() {
        startAngle = ShotRandomizer.getRandomStartAngle();
        clockwise = ShotRandomizer.getRandomDirection();
    }

    @Override
    public Vec3d updatePosition(float progress, float speedMultiplier, float tickDelta) {
        if (!isPlayerAvailable()) return Vec3d.ZERO;

        float currentAngle = startAngle + (progress * SWEEP_DEGREES * speedMultiplier * (clockwise ? 1 : -1));
        Vec3d offset = getCircularOffset(currentAngle, DISTANCE);

        return getPlayerPos(tickDelta).add(offset.x, HEIGHT, offset.z);
    }

    @Override
    public float updatePitch(float progress, float speedMultiplier, float tickDelta) {
        return 10f;
    }

    @Override
    public float updateYaw(float progress, float speedMultiplier, float tickDelta) {
        float currentAngle = startAngle + (progress * SWEEP_DEGREES * speedMultiplier * (clockwise ? 1 : -1));
        return currentAngle - 45f * (clockwise ? 1 : -1);
    }
}
