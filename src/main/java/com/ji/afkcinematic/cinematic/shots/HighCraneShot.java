package com.ji.afkcinematic.cinematic.shots;

import com.ji.afkcinematic.cinematic.CameraShot;
import com.ji.afkcinematic.cinematic.ShotRandomizer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.MathHelper;

/**
 * High crane shot — camera rises up and pulls back.
 * Distance: 6→12 blocks, Height: 4→15 blocks
 */
public class HighCraneShot implements CameraShot {
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
    public Vec3d updatePosition(float progress, float speedMultiplier, float tickDelta) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return Vec3d.ZERO;

        // Linear interpolation — constant speed, no easing
        float scaledProgress = Math.min(1.0f, progress * speedMultiplier);
        float currentDist = MathHelper.lerp(scaledProgress, START_DIST, END_DIST);
        float currentHeight = MathHelper.lerp(scaledProgress, START_HEIGHT, END_HEIGHT);

        double radians = Math.toRadians(startAngle);
        double offsetX = Math.cos(radians) * currentDist;
        double offsetZ = Math.sin(radians) * currentDist;

        return new Vec3d(
                client.player.getLerpedPos(tickDelta).x + offsetX,
                client.player.getLerpedPos(tickDelta).y + currentHeight,
                client.player.getLerpedPos(tickDelta).z + offsetZ
        );
    }

    @Override
    public float updatePitch(float progress, float speedMultiplier, float tickDelta) {
        // Linear pitch increase as camera rises
        float scaledProgress = Math.min(1.0f, progress * speedMultiplier);
        return MathHelper.lerp(scaledProgress, 20f, 45f);
    }

    @Override
    public float updateYaw(float progress, float speedMultiplier, float tickDelta) {
        return startAngle + 90f;
    }
}
