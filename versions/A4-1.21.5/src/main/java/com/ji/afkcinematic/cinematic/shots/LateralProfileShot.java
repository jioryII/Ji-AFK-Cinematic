package com.ji.afkcinematic.cinematic.shots;

import com.ji.afkcinematic.cinematic.AbstractCameraShot;
import com.ji.afkcinematic.cinematic.ShotRandomizer;
import net.minecraft.util.math.Vec3d;

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
    public Vec3d updatePosition(float progress, float speedMultiplier, float tickDelta) {
        if (!isPlayerAvailable()) return Vec3d.ZERO;

        float currentAngle = startAngle + (progress * ORBIT_DEGREES * speedMultiplier * (clockwise ? 1 : -1));
        Vec3d offset = getCircularOffset(currentAngle, DISTANCE);

        return getPlayerPos(tickDelta).add(offset.x, HEIGHT, offset.z);
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
