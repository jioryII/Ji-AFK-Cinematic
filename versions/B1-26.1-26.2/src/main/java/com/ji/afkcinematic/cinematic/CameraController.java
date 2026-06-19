package com.ji.afkcinematic.cinematic;

import com.ji.afkcinematic.cinematic.shots.*;
import com.ji.afkcinematic.config.ConfigManager;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import java.util.ArrayList;
import java.util.List;

public class CameraController {
    private static final List<CameraShot> SHOTS = new ArrayList<>();
    private static int currentShotIndex = 0;

    private static Vec3 exactFramePos = Vec3.ZERO;
    private static float exactFramePitch = 0f;
    private static float exactFrameYaw = 0f;

    public static void init() {
        SHOTS.clear();
        SHOTS.add(new LateralProfileShot());
        SHOTS.add(new FeetToFaceRiseShot());
        SHOTS.add(new PanoramaSweepShot());
        SHOTS.add(new LowOrbitShot());
        SHOTS.add(new DollyApproachShot());
        SHOTS.add(new OverShoulderShot());
        SHOTS.add(new AerialOrbitShot());
        SHOTS.add(new DistantHorizonShot());
        SHOTS.add(new DistantOrbitShot());
        SHOTS.add(new HighCraneShot());
    }

    public static void reset() {
        currentShotIndex = 0;
        Minecraft client = Minecraft.getInstance();
        if (client.player != null) {
            exactFramePos = client.player.getEyePosition();
            exactFramePitch = client.player.getXRot();
            exactFrameYaw = client.player.getYRot();
        }
    }

    public static void startShot(int index) {
        if (SHOTS.isEmpty()) return;
        currentShotIndex = index % SHOTS.size();
        SHOTS.get(currentShotIndex).start();
    }

    public static void evaluateFrame(float frameProgress, float tickDelta) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || SHOTS.isEmpty()) return;

        CameraShot currentShot = SHOTS.get(currentShotIndex);
        float speedMultiplier = ConfigManager.getConfig().cameraSpeed;

        Vec3 targetPos = currentShot.updatePosition(frameProgress, speedMultiplier, tickDelta);
        exactFramePitch = currentShot.updatePitch(frameProgress, speedMultiplier, tickDelta);
        exactFrameYaw = currentShot.updateYaw(frameProgress, speedMultiplier, tickDelta);

        Vec3 centerPos = client.player.getPosition(tickDelta).add(0, client.player.getEyeHeight(), 0);
        exactFramePos = CameraCollisionHelper.resolveCollision(centerPos, targetPos);
    }

    public static Vec3 getFramePos() { return exactFramePos; }
    public static float getFramePitch() { return exactFramePitch; }
    public static float getFrameYaw() { return exactFrameYaw; }
    
    public static int getShotCount() { return SHOTS.size(); }
}
