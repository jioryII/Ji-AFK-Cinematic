package com.ji.afkcinematic.cinematic.shots;

import com.ji.afkcinematic.cinematic.AbstractCameraShot;
import com.ji.afkcinematic.cinematic.ShotRandomizer;
import net.minecraft.world.phys.Vec3;

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
    public Vec3 updatePosition(float progress, float speedMultiplier, float tickDelta) {
        if (!isPlayerAvailable()) return Vec3.ZERO;

        float currentAngle = startAngle + (progress * SWEEP_DEGREES * speedMultiplier * (clockwise ? 1 : -1));
        Vec3 offset = getCircularOffset(currentAngle, DISTANCE);

        return new Vec3(
                getPlayerPos(tickDelta).x + offset.x,
                getPlayerPos(tickDelta).y + HEIGHT,
                getPlayerPos(tickDelta).z + offset.z
        );
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
