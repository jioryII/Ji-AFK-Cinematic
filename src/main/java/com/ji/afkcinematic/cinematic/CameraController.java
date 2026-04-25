package com.ji.afkcinematic.cinematic;

import com.ji.afkcinematic.cinematic.shots.*;
import com.ji.afkcinematic.config.ConfigManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.Vec3d;
import java.util.ArrayList;
import java.util.List;

public class CameraController {
    private static final List<CameraShot> SHOTS = new ArrayList<>();
    private static int currentShotIndex = 0;

    // Evaluated coordinate for the exact frame
    private static Vec3d exactFramePos = Vec3d.ZERO;
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
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            exactFramePos = client.player.getEyePos();
            exactFramePitch = client.player.getPitch();
            exactFrameYaw = client.player.getYaw();
        }
    }

    public static void startShot(int index) {
        currentShotIndex = index % SHOTS.size();
        SHOTS.get(currentShotIndex).start();
    }

    /**
     * Evaluates the cinematic state directly at the given frame parametric progress.
     * @param frameProgress 0.0 to 1.0 exact percentage through the cinematic shot.
     * @param tickDelta sub-tick delta for interpolating the player's moving position
     */
    public static void evaluateFrame(float frameProgress, float tickDelta) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        CameraShot currentShot = SHOTS.get(currentShotIndex);
        float speedMultiplier = ConfigManager.getConfig().cameraSpeed;

        // Directly evaluate parametric coordinates
        Vec3d targetPos = currentShot.updatePosition(frameProgress, speedMultiplier, tickDelta);
        exactFramePitch = currentShot.updatePitch(frameProgress, speedMultiplier, tickDelta);
        exactFrameYaw = currentShot.updateYaw(frameProgress, speedMultiplier, tickDelta);

        // Raycast logic every frame guarantees total smoothness
        // We also use lerped eye position to avoid jittering
        Vec3d centerPos = new Vec3d(
                client.player.getLerpedPos(tickDelta).x,
                client.player.getLerpedPos(tickDelta).y + client.player.getStandingEyeHeight(),
                client.player.getLerpedPos(tickDelta).z
        );
        exactFramePos = CameraCollisionHelper.resolveCollision(centerPos, targetPos);
    }

    public static Vec3d getFramePos() { return exactFramePos; }
    public static float getFramePitch() { return exactFramePitch; }
    public static float getFrameYaw() { return exactFrameYaw; }
    
    public static int getShotCount() { return SHOTS.size(); }
}
