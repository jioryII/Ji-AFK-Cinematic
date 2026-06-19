package com.ji.afkcinematic.cinematic.shots;

import com.ji.afkcinematic.cinematic.AbstractCameraShot;


import com.ji.afkcinematic.cinematic.ShotRandomizer;
import net.minecraft.util.math.Vec3d;

/**
 * Dolly approach — camera moves closer to the player linearly.
 * Distance: 8→3 blocks, Height: 3 blocks
 */
public class DollyApproachShot extends AbstractCameraShot {
    private float startAngle;

    private static final float START_DIST = 8f;
    private static final float END_DIST = 3f;
    private static final float HEIGHT = 3f;

    @Override
    public void start() {
        startAngle = ShotRandomizer.getRandomStartAngle();
    }

    @Override
    public Vec3d updatePosition(float progress, float speedMultiplier, float tickDelta) {
        if (!isPlayerAvailable()) return Vec3d.ZERO;

        float scaledProgress = Math.min(1.0f, progress * speedMultiplier);
        float currentDist = lerp(scaledProgress, START_DIST, END_DIST);

        Vec3d offset = getCircularOffset(startAngle, currentDist);

        return getPlayerPos(tickDelta).add(offset.x, HEIGHT, offset.z);
    }

    @Override
    public float updatePitch(float progress, float speedMultiplier, float tickDelta) {
        return 10f;
    }

    @Override
    public float updateYaw(float progress, float speedMultiplier, float tickDelta) {
        return startAngle + 90f + 45f;
    }
}
