package com.ji.afkcinematic.cinematic;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public abstract class AbstractCameraShot implements CameraShot {
    protected final MinecraftClient client = MinecraftClient.getInstance();

    @Override
    public void start() {}

    protected Vec3d getPlayerPos(float tickDelta) {
        ClientPlayerEntity player = client.player;
        return player != null ? player.getLerpedPos(tickDelta) : Vec3d.ZERO;
    }

    protected boolean isPlayerAvailable() {
        return client.player != null;
    }

    protected float lerp(float progress, float start, float end) {
        return MathHelper.lerp(progress, start, end);
    }

    // Highly optimized using Minecraft's Fast Math Lookup Tables
    protected Vec3d getCircularOffset(float angleDegrees, float distance) {
        float rads = angleDegrees * 0.017453292F; // Radians per degree
        // MathHelper uses a fast 65536-entry array lookup instead of slow native FPU instructions
        return new Vec3d(MathHelper.cos(rads) * distance, 0, MathHelper.sin(rads) * distance);
    }

    @Override
    public abstract Vec3d updatePosition(float progress, float speedMultiplier, float tickDelta);

    @Override
    public abstract float updatePitch(float progress, float speedMultiplier, float tickDelta);

    @Override
    public abstract float updateYaw(float progress, float speedMultiplier, float tickDelta);
}
