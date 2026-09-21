package com.ji.afkcinematic.cinematic.shots;

import com.ji.afkcinematic.cinematic.AbstractCameraShot;
import com.ji.afkcinematic.cinematic.ShotRandomizer;
import net.minecraft.world.phys.Vec3;

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
    public Vec3 updatePosition(float progress, float speedMultiplier, float tickDelta) {
        if (!isPlayerAvailable()) return Vec3.ZERO;

        float scaledProgress = progress * speedMultiplier;
        float currentAngle = startAngle + (scaledProgress * 5f);
        float currentDist = 3f + (scaledProgress * 1.5f);

        Vec3 offset = getCircularOffset(currentAngle, currentDist);

        return new Vec3(
                getPlayerPos(tickDelta).x + offset.x,
                getPlayerPos(tickDelta).y + 2.5f,
                getPlayerPos(tickDelta).z + offset.z
        );
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
