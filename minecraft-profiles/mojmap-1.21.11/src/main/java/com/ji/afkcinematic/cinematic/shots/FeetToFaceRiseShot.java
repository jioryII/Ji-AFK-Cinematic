package com.ji.afkcinematic.cinematic.shots;

import com.ji.afkcinematic.cinematic.AbstractCameraShot;
import com.ji.afkcinematic.cinematic.ShotRandomizer;
import net.minecraft.world.phys.Vec3;

/**
 * Camera rises from feet to face level — linear constant speed.
 * Distance: 6→4 blocks (close), Height: 0.5→4 blocks
 */
public class FeetToFaceRiseShot extends AbstractCameraShot {
    private float startAngle;

    private static final float START_DIST = 6f;
    private static final float END_DIST = 4f;
    private static final float START_HEIGHT = 0.5f;
    private static final float END_HEIGHT = 4f;

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
        return lerp(scaledProgress, -20f, 0f);
    }

    @Override
    public float updateYaw(float progress, float speedMultiplier, float tickDelta) {
        return startAngle + 90f;
    }
}
