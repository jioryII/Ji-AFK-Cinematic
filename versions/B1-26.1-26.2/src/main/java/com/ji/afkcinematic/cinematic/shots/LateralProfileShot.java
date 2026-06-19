package com.ji.afkcinematic.cinematic.shots;

import com.ji.afkcinematic.cinematic.AbstractCameraShot;
import com.ji.afkcinematic.cinematic.ShotRandomizer;
import net.minecraft.world.phys.Vec3;

/**
 * Slow lateral orbit around the player at eye level.
 * Distance: 5 blocks, Height: 3 blocks, Orbit: 35°
 */
public class LateralProfileShot extends AbstractCameraShot {
    private float startAngle;
    private boolean clockwise;

    private static final float DISTANCE = 5f;
    private static final float HEIGHT = 3f;
    private static final float ORBIT_DEGREES = 35f;

    @Override
    public void start() {
        startAngle = ShotRandomizer.getRandomStartAngle();
        clockwise = ShotRandomizer.getRandomDirection();
    }

    @Override
    public Vec3 updatePosition(float progress, float speedMultiplier, float tickDelta) {
        if (!isPlayerAvailable()) return Vec3.ZERO;

        float currentAngle = startAngle + (progress * ORBIT_DEGREES * speedMultiplier * (clockwise ? 1 : -1));
        Vec3 offset = getCircularOffset(currentAngle, DISTANCE);

        return new Vec3(
                getPlayerPos(tickDelta).x + offset.x,
                getPlayerPos(tickDelta).y + HEIGHT,
                getPlayerPos(tickDelta).z + offset.z
        );
    }

    @Override
    public float updatePitch(float progress, float speedMultiplier, float tickDelta) {
        return 15f;
    }

    @Override
    public float updateYaw(float progress, float speedMultiplier, float tickDelta) {
        float currentAngle = startAngle + (progress * ORBIT_DEGREES * speedMultiplier * (clockwise ? 1 : -1));
        return currentAngle + 90f;
    }
}
