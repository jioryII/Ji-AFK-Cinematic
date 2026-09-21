package com.ji.afkcinematic.cinematic.shots;

import com.ji.afkcinematic.cinematic.AbstractCameraShot;
import com.ji.afkcinematic.cinematic.ShotRandomizer;
import net.minecraft.world.phys.Vec3;

/**
 * High crane shot — camera rises up and pulls back.
 * Distance: 6→12 blocks, Height: 4→15 blocks
 */
public class HighCraneShot extends AbstractCameraShot {
    private float startAngle;

    private static final float START_DIST = 6f;
    private static final float END_DIST = 12f;
    private static final float START_HEIGHT = 4f;
    private static final float END_HEIGHT = 15f;

    @Override
    public void start() {
        startAngle = ShotRandomizer.getRandomStartAngle();
    }

    @Override
    public Vec3 updatePosition(float progress, float speedMultiplier, float tickDelta) {
        if (!isPlayerAvailable()) return Vec3.ZERO;

        float scaledProgress = Math.min(1.0f, progress * speedMultiplier);
        float currentDist = lerp(scaledProgress, START_DIST, END_DIST);
        float currentHeight = lerp(scaledProgress, START_HEIGHT, END_HEIGHT);

        Vec3 offset = getCircularOffset(startAngle, currentDist);

        return new Vec3(
                getPlayerPos(tickDelta).x + offset.x,
                getPlayerPos(tickDelta).y + currentHeight,
                getPlayerPos(tickDelta).z + offset.z
        );
    }

    @Override
    public float updatePitch(float progress, float speedMultiplier, float tickDelta) {
        float scaledProgress = Math.min(1.0f, progress * speedMultiplier);
        return lerp(scaledProgress, 20f, 45f);
    }

    @Override
    public float updateYaw(float progress, float speedMultiplier, float tickDelta) {
        return startAngle + 90f;
    }
}
