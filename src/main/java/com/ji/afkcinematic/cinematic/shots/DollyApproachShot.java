package com.ji.afkcinematic.cinematic.shots;

import com.ji.afkcinematic.cinematic.CameraShot;
import com.ji.afkcinematic.cinematic.ShotRandomizer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.MathHelper;

/**
 * Dolly approach — camera moves closer to the player linearly.
 * Distance: 8→3 blocks, Height: 3 blocks
 */
public class DollyApproachShot implements CameraShot {
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
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return Vec3d.ZERO;

        // Linear interpolation — constant speed, no easing
        float scaledProgress = Math.min(1.0f, progress * speedMultiplier);
        float currentDist = MathHelper.lerp(scaledProgress, START_DIST, END_DIST);

        double radians = Math.toRadians(startAngle);
        double offsetX = Math.cos(radians) * currentDist;
        double offsetZ = Math.sin(radians) * currentDist;

        return new Vec3d(
                client.player.getLerpedPos(tickDelta).x + offsetX,
                client.player.getLerpedPos(tickDelta).y + HEIGHT,
                client.player.getLerpedPos(tickDelta).z + offsetZ
        );
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
