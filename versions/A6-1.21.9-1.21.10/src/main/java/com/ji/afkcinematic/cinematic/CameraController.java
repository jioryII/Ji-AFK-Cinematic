package com.ji.afkcinematic.cinematic;

import com.ji.afkcinematic.cinematic.shots.*;
import com.ji.afkcinematic.config.ConfigManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.Vec3d;
import java.util.ArrayList;
import java.util.List;

public class CameraController {
    private static final List<CameraShot> CHARACTER_SHOTS = new ArrayList<>();
    private static final List<CameraShot> ENVIRONMENT_SHOTS = new ArrayList<>();
    private static final List<CameraShot> ACTIVE_SEQUENCE = new ArrayList<>();
    private static float[] activeRotationAngles = new float[0];
    private static int currentShotIndex = 0;

    private static Vec3d exactFramePos = Vec3d.ZERO;
    private static float exactFramePitch = 0f;
    private static float exactFrameYaw = 0f;
    private static float exactFrameRoll = 0f;

    public static void init() {
        CHARACTER_SHOTS.clear();
        for (ComposedCharacterShot.Preset preset : ComposedCharacterShot.Preset.values()) {
            CHARACTER_SHOTS.add(new ComposedCharacterShot(preset));
        }

        ENVIRONMENT_SHOTS.clear();
        for (EnvironmentalShot.Preset preset : EnvironmentalShot.Preset.values()) {
            ENVIRONMENT_SHOTS.add(new EnvironmentalShot(preset));
        }
        prepareSequence(ConfigManager.getConfig().characterShotPercentage, System.nanoTime());
    }

    public static void prepareSequence(int characterPercentage) {
        prepareSequence(characterPercentage, System.nanoTime());
    }

    static void prepareSequence(int characterPercentage, long seed) {
        ACTIVE_SEQUENCE.clear();
        int[] plan = ShotSequencePlanner.plan(characterPercentage,
                CHARACTER_SHOTS.size(), ENVIRONMENT_SHOTS.size(), seed);
        activeRotationAngles = CameraRotationProfile.plan(plan.length, seed);
        for (int poolIndex : plan) {
            if (poolIndex < CHARACTER_SHOTS.size()) {
                ACTIVE_SEQUENCE.add(CHARACTER_SHOTS.get(poolIndex));
            } else {
                ACTIVE_SEQUENCE.add(ENVIRONMENT_SHOTS.get(poolIndex - CHARACTER_SHOTS.size()));
            }
        }
    }

    public static void reset() {
        currentShotIndex = 0;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            exactFramePos = client.player.getEyePos();
            exactFramePitch = client.player.getPitch();
            exactFrameYaw = client.player.getYaw();
            exactFrameRoll = 0f;
        }
    }

    public static void startShot(int index) {
        if (ACTIVE_SEQUENCE.isEmpty()) return;
        currentShotIndex = Math.floorMod(index, ACTIVE_SEQUENCE.size());
        ACTIVE_SEQUENCE.get(currentShotIndex).start();
    }

    public static void evaluateFrame(float frameProgress, float tickDelta) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || ACTIVE_SEQUENCE.isEmpty()) return;

        CameraShot currentShot = ACTIVE_SEQUENCE.get(currentShotIndex);
        var config = ConfigManager.getConfig();
        float speedMultiplier = config.cameraSpeed;
        float elapsedSeconds = ShotMotion.elapsedSeconds(frameProgress, config.shotDurationSeconds);
        Vec3d targetPos = currentShot.updatePosition(elapsedSeconds, speedMultiplier, tickDelta);
        exactFramePitch = currentShot.updatePitch(elapsedSeconds, speedMultiplier, tickDelta);
        exactFrameYaw = currentShot.updateYaw(elapsedSeconds, speedMultiplier, tickDelta);
        float rotationAngle = currentShotIndex < activeRotationAngles.length
                ? activeRotationAngles[currentShotIndex] : 0.0f;
        exactFrameRoll = CameraRotationProfile.apply(rotationAngle, config.cameraRotationEnabled);

        Vec3d centerPos = client.player.getLerpedPos(tickDelta)
                .add(0, client.player.getEyeHeight(client.player.getPose()), 0);
        exactFramePos = CameraCollisionHelper.resolveCollision(centerPos, targetPos);
    }

    public static Vec3d getFramePos() { return exactFramePos; }
    public static float getFramePitch() { return exactFramePitch; }
    public static float getFrameYaw() { return exactFrameYaw; }
    public static float getFrameRoll() { return exactFrameRoll; }
    public static int getShotCount() { return ACTIVE_SEQUENCE.size(); }
    public static int getCharacterPresetCount() { return CHARACTER_SHOTS.size(); }
    public static int getEnvironmentPresetCount() { return ENVIRONMENT_SHOTS.size(); }
    public static int getActiveEnvironmentShotCount() {
        return (int) ACTIVE_SEQUENCE.stream()
                .filter(shot -> shot.getId().startsWith("environment/"))
                .count();
    }
    public static int getActiveCharacterShotCount() {
        return ACTIVE_SEQUENCE.size() - getActiveEnvironmentShotCount();
    }
    public static String getCurrentShotId() {
        return ACTIVE_SEQUENCE.isEmpty() ? "none" : ACTIVE_SEQUENCE.get(currentShotIndex).getId();
    }
}
