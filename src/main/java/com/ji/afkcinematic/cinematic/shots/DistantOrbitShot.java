package com.ji.afkcinematic.cinematic.shots;

import com.ji.afkcinematic.cinematic.CameraShot;
import com.ji.afkcinematic.cinematic.ShotRandomizer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.Vec3d;

/**
 * Distant orbit — wider orbit showing the environment from elevation.
 * Distance: 15 blocks, Height: 12 blocks, Orbit: 20°
 */
public class DistantOrbitShot implements CameraShot {
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
    public Vec3d updatePosition(float progress, float speedMultiplier, float tickDelta) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return Vec3d.ZERO;

        float currentAngle = startAngle + (progress * ORBIT_DEGREES * speedMultiplier * (clockwise ? 1 : -1));

        double radians = Math.toRadians(currentAngle);
        double offsetX = Math.cos(radians) * DISTANCE;
        double offsetZ = Math.sin(radians) * DISTANCE;

        return new Vec3d(
                client.player.getLerpedPos(tickDelta).x + offsetX,
                client.player.getLerpedPos(tickDelta).y + HEIGHT,
                client.player.getLerpedPos(tickDelta).z + offsetZ
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
