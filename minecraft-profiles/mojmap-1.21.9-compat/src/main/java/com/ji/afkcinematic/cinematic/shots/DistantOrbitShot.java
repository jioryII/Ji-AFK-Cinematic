package com.ji.afkcinematic.cinematic.shots;

import com.ji.afkcinematic.cinematic.AbstractCameraShot;
import com.ji.afkcinematic.cinematic.ShotRandomizer;
import net.minecraft.world.phys.Vec3;

/**
 * Distant orbit — wider orbit showing the environment from elevation.
 * Distance: 15 blocks, Height: 12 blocks, Orbit: 20°
 */
public class DistantOrbitShot extends AbstractCameraShot {
    private float startAngle;
    private boolean clockwise;

    private static final float DISTANCE = 15f;
    private static final float HEIGHT = 12f;
    private static final float ORBIT_DEGREES = 20f;

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
        return 28f;
    }

    @Override
    public float updateYaw(float progress, float speedMultiplier, float tickDelta) {
        float currentAngle = startAngle + (progress * ORBIT_DEGREES * speedMultiplier * (clockwise ? 1 : -1));
        return currentAngle + 90f;
    }
}
