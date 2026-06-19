package com.ji.afkcinematic.cinematic;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public abstract class AbstractCameraShot implements CameraShot {
    protected final Minecraft client = Minecraft.getInstance();

    @Override
    public void start() {}

    protected Vec3 getPlayerPos(float tickDelta) {
        LocalPlayer player = client.player;
        return player != null ? player.getPosition(tickDelta) : Vec3.ZERO;
    }

    protected boolean isPlayerAvailable() {
        return client.player != null;
    }

    protected float lerp(float progress, float start, float end) {
        return Mth.lerp(progress, start, end);
    }

    protected Vec3 getCircularOffset(float angleDegrees, float distance) {
        float rads = angleDegrees * 0.017453292F; 
        return new Vec3(Mth.cos(rads) * distance, 0, Mth.sin(rads) * distance);
    }

    @Override
    public abstract Vec3 updatePosition(float progress, float speedMultiplier, float tickDelta);

    @Override
    public abstract float updatePitch(float progress, float speedMultiplier, float tickDelta);

    @Override
    public abstract float updateYaw(float progress, float speedMultiplier, float tickDelta);
}
