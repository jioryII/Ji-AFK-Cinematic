package com.ji.afkcinematic.cinematic.shots;

import com.ji.afkcinematic.cinematic.CameraShot;
import com.ji.afkcinematic.cinematic.ShotRandomizer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.MathHelper;

/**
 * Camera rises from feet to face level — linear constant speed.
 * Distance: 6→4 blocks (close), Height: 0.5→4 blocks
 */
public class FeetToFaceRiseShot implements CameraShot {
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
        // Linear pitch change from looking up at feet to looking level
        float scaledProgress = Math.min(1.0f, progress * speedMultiplier);
        return MathHelper.lerp(scaledProgress, -20f, 0f);
    }

    @Override
    public float updateYaw(float progress, float speedMultiplier, float tickDelta) {
        return startAngle + 90f;
    }
}
