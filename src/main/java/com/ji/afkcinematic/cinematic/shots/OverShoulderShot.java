package com.ji.afkcinematic.cinematic.shots;

import com.ji.afkcinematic.cinematic.CameraShot;
import com.ji.afkcinematic.cinematic.ShotRandomizer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.Vec3d;

/**
 * Over-the-shoulder shot — close behind/beside the player looking forward.
 * Slow drift with slight distance increase.
 */
public class OverShoulderShot implements CameraShot {
    private float startAngle;

    @Override
    public void start() {
        startAngle = ShotRandomizer.getRandomStartAngle();
    }

    @Override
    public Vec3d updatePosition(float progress, float speedMultiplier, float tickDelta) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return Vec3d.ZERO;

        // Constant slow drift, scaled by speed multiplier
        float scaledProgress = progress * speedMultiplier;
        float currentAngle = startAngle + (scaledProgress * 5f);
        float currentDist = 3f + (scaledProgress * 1.5f);

        double radians = Math.toRadians(currentAngle);
        double offsetX = Math.cos(radians) * currentDist;
        double offsetZ = Math.sin(radians) * currentDist;

        return new Vec3d(
                client.player.getLerpedPos(tickDelta).x + offsetX,
                client.player.getLerpedPos(tickDelta).y + 2.5f,
                client.player.getLerpedPos(tickDelta).z + offsetZ
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
