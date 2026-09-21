package com.ji.afkcinematic.cinematic;

import com.ji.afkcinematic.cinematic.shots.*;
import com.ji.afkcinematic.config.ConfigManager;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import java.util.ArrayList;
import java.util.List;

public class CameraController {
    private static final List<CameraShot> CHARACTER_SHOTS = new ArrayList<>();
    private static final List<CameraShot> ENVIRONMENT_SHOTS = new ArrayList<>();
    private static final List<CameraShot> ACTIVE_SEQUENCE = new ArrayList<>();
    private static final FishingShot FISHING_SHOT = new FishingShot();
    private static float[] activeRotationAngles = new float[0];
    private static int currentShotIndex = 0;
    private static boolean biteActive;

    private static Vec3 exactFramePos = Vec3.ZERO;
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
        biteActive = false;
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
        chooseOpenFirstShot();
    }

    public static void setBiteActive(boolean active) {
        biteActive = active;
        if (active) FISHING_SHOT.start();
    }

    public static void beginFishingFocus(Vec3 approachParticle) {
        biteActive = true;
        FISHING_SHOT.start(approachParticle);
    }

    private static void chooseOpenFirstShot() {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || ACTIVE_SEQUENCE.isEmpty()) return;
        Vec3 eye = client.player.getEyePosition();
        int best = 0;
        double bestScore = -1.0;
        for (int i = 0; i < ACTIVE_SEQUENCE.size(); i++) {
            CameraShot shot = ACTIVE_SEQUENCE.get(i);
            shot.start();
            double minimum = 1.0;
            double average = 0.0;
            for (int sample = 0; sample < 5; sample++) {
                Vec3 desired = shot.updatePosition(sample * 2.0f, 1.0f, 0);
                double full = desired.distanceTo(eye);
                double clearance = full < 0.01 ? 0
                        : CameraCollisionHelper.resolveCollision(eye, desired).distanceTo(eye) / full;
                minimum = Math.min(minimum, clearance);
                average += clearance / 5.0;
            }
            double score = minimum + average * 0.01;
            if (score > bestScore) { bestScore = score; best = i; }
        }
        CameraShot first = ACTIVE_SEQUENCE.get(0);
        ACTIVE_SEQUENCE.set(0, ACTIVE_SEQUENCE.get(best));
        ACTIVE_SEQUENCE.set(best, first);
    }

    public static void reset() {
        currentShotIndex = 0;
        Minecraft client = Minecraft.getInstance();
        if (client.player != null) {
            exactFramePos = client.player.getEyePosition();
            exactFramePitch = client.player.getXRot();
            exactFrameYaw = client.player.getYRot();
            exactFrameRoll = 0f;
        }
    }

    public static void startShot(int index) {
        if (ACTIVE_SEQUENCE.isEmpty()) return;
        currentShotIndex = Math.floorMod(index, ACTIVE_SEQUENCE.size());
        ACTIVE_SEQUENCE.get(currentShotIndex).start();
    }

    public static void evaluateFrame(float frameProgress, float tickDelta) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || ACTIVE_SEQUENCE.isEmpty()) return;

        CameraShot currentShot = biteActive ? FISHING_SHOT : ACTIVE_SEQUENCE.get(currentShotIndex);
        var config = ConfigManager.getConfig();
        float speedMultiplier = config.cameraSpeed;
        float elapsedSeconds = biteActive
                ? CinematicManager.getBiteTicks() / 20.0f
                : ShotMotion.elapsedSeconds(frameProgress, config.shotDurationSeconds);
        Vec3 targetPos = currentShot.updatePosition(elapsedSeconds, speedMultiplier, tickDelta);
        exactFramePitch = currentShot.updatePitch(elapsedSeconds, speedMultiplier, tickDelta);
        exactFrameYaw = currentShot.updateYaw(elapsedSeconds, speedMultiplier, tickDelta);
        float rotationAngle = !biteActive && currentShotIndex < activeRotationAngles.length
                ? activeRotationAngles[currentShotIndex] : 0.0f;
        exactFrameRoll = CameraRotationProfile.apply(rotationAngle, config.cameraRotationEnabled);

        Vec3 centerPos = client.player.getPosition(tickDelta)
                .add(0, client.player.getEyeHeight(client.player.getPose()), 0);
        exactFramePos = CameraCollisionHelper.resolveCollision(centerPos, targetPos);
    }

    public static Vec3 getFramePos() { return exactFramePos; }
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
        return biteActive ? FISHING_SHOT.getId()
                : ACTIVE_SEQUENCE.isEmpty() ? "none" : ACTIVE_SEQUENCE.get(currentShotIndex).getId();
    }
}
