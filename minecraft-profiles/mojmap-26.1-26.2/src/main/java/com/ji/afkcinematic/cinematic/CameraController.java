package com.ji.afkcinematic.cinematic;

import com.ji.afkcinematic.cinematic.shots.*;
import com.ji.afkcinematic.config.ConfigManager;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import java.util.ArrayList;
import java.util.List;
import com.ji.afkcinematic.cinematic.director.CompositionDirector;
import com.ji.afkcinematic.cinematic.director.CompositionPath;

public class CameraController {
    private static final List<CameraShot> CHARACTER_SHOTS = new ArrayList<>();
    private static final List<CameraShot> ENVIRONMENT_SHOTS = new ArrayList<>();
    private static final List<CameraShot> ACTIVE_SEQUENCE = new ArrayList<>();
    private static final FishingShot FISHING_SHOT = new FishingShot();
    private static float[] activeRotationAngles = new float[0];
    private static int currentShotIndex = 0;
    private static boolean biteActive;
    private static final CompositionDirector DIRECTOR = new CompositionDirector();
    private static CompositionDirector.Selection selection;
    private static CameraShot activeShot;
    private static final com.ji.afkcinematic.cinematic.director.CameraTravel TRAVEL = new com.ji.afkcinematic.cinematic.director.CameraTravel();
    private static final com.ji.afkcinematic.cinematic.director.HorizonAwards HORIZONS = new com.ji.afkcinematic.cinematic.director.HorizonAwards();
    private static int editCycle;
    private static Object memoryWorld;
    private static long lastFrameTime;
    private static long focusTake;
    private static final CameraAim AIM = new CameraAim();
    private static double horizonYaw = Double.NaN;

    private static final CameraFrameStore FRAMES = new CameraFrameStore();

    public static void init() {
        CHARACTER_SHOTS.clear();
        for (ComposedCharacterShot.Preset preset : ComposedCharacterShot.Preset.values()) {
            CHARACTER_SHOTS.add(new ComposedCharacterShot(preset));
        }

        ENVIRONMENT_SHOTS.clear();
        for (EnvironmentalShot.Preset preset : EnvironmentalShot.Preset.values()) {
            ENVIRONMENT_SHOTS.add(new EnvironmentalShot(preset));
        }
        prepareSequence(ConfigManager.getConfig().characterShotPercentage, com.ji.afkcinematic.random.RandomStreams.seed(com.ji.afkcinematic.random.RandomStreams.Domain.CAMERA_PLAN));
    }

    public static void prepareSequence(int characterPercentage) {
        prepareSequence(characterPercentage, com.ji.afkcinematic.random.RandomStreams.seed(com.ji.afkcinematic.random.RandomStreams.Domain.CAMERA_PLAN));
    }

    static void prepareSequence(int characterPercentage, long seed) {
        rememberExit(); HORIZONS.reset(); editCycle = 0;
        planSequence(characterPercentage, seed, "none");
    }

    /** Refresh the montage while preserving recent composition history across cycles. */
    public static void prepareNextCycle(int characterPercentage) {
        rememberExit();
        editCycle++;
        planSequence(characterPercentage, com.ji.afkcinematic.random.RandomStreams.seed(com.ji.afkcinematic.random.RandomStreams.Domain.CAMERA_PLAN), getCurrentShotId());
    }

    private static void planSequence(int characterPercentage, long seed, String previousId) {
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
        if (ACTIVE_SEQUENCE.size() > 1 && ACTIVE_SEQUENCE.get(0).getId().equals(previousId)) {
            java.util.Collections.swap(ACTIVE_SEQUENCE, 0, 1);
        }
        activeShot = null;
        selection = null;
    }

    public static void setBiteActive(boolean active) {
        focusTake++;
        biteActive = active;
        TRAVEL.reset(); AIM.reset(); lastFrameTime = 0;
        if (active) FISHING_SHOT.start();
    }

    public static void beginFishingFocus(Vec3 approachParticle) {
        focusTake++;
        biteActive = true; TRAVEL.reset(); AIM.reset(); lastFrameTime = 0;
        FISHING_SHOT.start(approachParticle);
    }

    public static void reset() {
        focusTake++;
        rememberExit();
        currentShotIndex = 0; TRAVEL.reset(); AIM.reset(); lastFrameTime = 0;
        activeShot = null;
        selection = null;
        FRAMES.reset();
        Minecraft client = Minecraft.getInstance();
        if (client.player != null) {
            Vec3 eye = client.player.getEyePosition();
            FRAMES.publish(new CameraFrame(eye.x, eye.y, eye.z,
                    client.player.getXRot(), client.player.getYRot(), 0f));
        }
    }

    private static void rememberExit() {
        Minecraft client = Minecraft.getInstance();
        if (memoryWorld != client.level) { DIRECTOR.reset(); memoryWorld = client.level; return; }
        if (selection != null && client.player != null) DIRECTOR.observeExit(FRAMES.current(), DirectedCameraShot.subject(1));
    }

    public static void startShot(int index) {
        focusTake++;
        rememberExit();
        if (ACTIVE_SEQUENCE.isEmpty()) return;
        TRAVEL.reset(); AIM.reset(); lastFrameTime = 0;
        currentShotIndex = Math.floorMod(index, ACTIVE_SEQUENCE.size());
        activeShot = ACTIVE_SEQUENCE.get(currentShotIndex);
        activeShot.start();
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || !(activeShot instanceof DirectedCameraShot requested)) return;
        var config = ConfigManager.getConfig();
        horizonYaw = client.level == null || !client.level.dimensionType().hasSkyLight() ? Double.NaN : HORIZONS.heading(client.level.getDefaultClockTime(), editCycle, com.ji.afkcinematic.random.RandomStreams.seed(com.ji.afkcinematic.random.RandomStreams.Domain.HORIZON), requested.preset().range());
        selection = Double.isFinite(horizonYaw) ? DIRECTOR.selectHorizon(requested.preset(), DirectedCameraShot.subject(1),
                horizonYaw, config.shotDurationSeconds, config.cameraSpeed, com.ji.afkcinematic.random.RandomStreams.seed(com.ji.afkcinematic.random.RandomStreams.Domain.CAMERA_VIEW), CameraCollisionHelper::clearDistance) : DIRECTOR.select(requested.preset(), DirectedCameraShot.subject(1), client.player.getYRot(),
                config.shotDurationSeconds, config.cameraSpeed, com.ji.afkcinematic.random.RandomStreams.seed(com.ji.afkcinematic.random.RandomStreams.Domain.CAMERA_VIEW), CameraCollisionHelper::clearDistance);
        if (!selection.fallback()) {
            DirectedCameraShot directed = selection.path().preset() == requested.preset()
                    ? requested : new DirectedCameraShot(selection.path().preset());
            directed.usePath(selection.path());
            activeShot = directed;
        }
    }

    public static CameraFrame evaluateFrame(float frameProgress, float tickDelta) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || ACTIVE_SEQUENCE.isEmpty()) return null;

        CameraShot currentShot = biteActive ? FISHING_SHOT
                : activeShot != null ? activeShot : ACTIVE_SEQUENCE.get(currentShotIndex);
        var config = ConfigManager.getConfig();
        float speedMultiplier = config.cameraSpeed;
        float elapsedSeconds = biteActive
                ? CinematicManager.getBiteTicks() / 20.0f
                : ShotMotion.elapsedSeconds(frameProgress, config.shotDurationSeconds);
        float rotationAngle = !biteActive && currentShotIndex < activeRotationAngles.length
                ? activeRotationAngles[currentShotIndex] : 0.0f;
        float roll = CameraRotationProfile.apply(rotationAngle, config.cameraRotationEnabled);

        Vec3 centerPos = client.player.getPosition(tickDelta)
                .add(0, client.player.getEyeHeight(client.player.getPose()), 0);
        if (!biteActive && selection != null && selection.fallback()) {
            CameraFrame fallback = selection.fallbackFrame();
            return FRAMES.publish(new CameraFrame(centerPos.x, centerPos.y, centerPos.z,
                    fallback.pitch(), fallback.yaw(), 0));
        }
        CompositionPath.Pose directedPose = currentShot instanceof DirectedCameraShot directed
                ? directed.pose(elapsedSeconds, speedMultiplier, tickDelta) : null;
        Vec3 targetPos = directedPose != null ? DirectedCameraShot.nativeVector(directedPose.position())
                : currentShot.updatePosition(elapsedSeconds, speedMultiplier, tickDelta);
        long now = System.nanoTime();
        double dt = lastFrameTime == 0 ? 0 : (now - lastFrameTime) / 1.0e9;
        lastFrameTime = now;
        Vec3 resolved = DirectedCameraShot.nativeVector(TRAVEL.move(DirectedCameraShot.vector(targetPos), directedPose != null ? DirectedCameraShot.vector(client.player.getPosition(tickDelta)) : com.ji.afkcinematic.cinematic.director.CameraVector.ZERO, dt, CameraCollisionHelper::clearDistance));
        CameraFrame aimed;
        if (directedPose != null) {
            aimed = directedPose.at(DirectedCameraShot.vector(resolved));
        } else {
            Vec3 focus = currentShot == FISHING_SHOT ? FISHING_SHOT.getFocusTarget() : null;
            aimed = focus == null ? new CameraFrame(resolved.x, resolved.y, resolved.z,
                    currentShot.updatePitch(elapsedSeconds, speedMultiplier, tickDelta),
                    currentShot.updateYaw(elapsedSeconds, speedMultiplier, tickDelta), 0)
                    : new CompositionPath.Pose(DirectedCameraShot.vector(targetPos),
                            DirectedCameraShot.vector(focus), 0, 0).at(DirectedCameraShot.vector(resolved));
        }
        CameraFrame candidate = AIM.track(new CameraFrame(resolved.x, resolved.y, resolved.z, aimed.pitch(), aimed.yaw(), roll), dt);
        CameraFrame published = FRAMES.publish(candidate);
        if (published == null) {
            com.ji.afkcinematic.JiAFKCinematic.LOGGER.warn(
                    "Skipping non-finite camera frame (pos={}, pitch={}, yaw={})", resolved, aimed.pitch(), aimed.yaw());
        }
        return published;
    }

    public static long getFocusTake() { return focusTake; }
    /** Semantic optical target; environment views deliberately have no synthetic aim distance. */
    public static Vec3 getOpticalFocusTarget(float partialTick) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || !(activeShot instanceof DirectedCameraShot directed)
                || !FocusShotPolicy.usesDepth(directed.preset(), biteActive)) return null;
        boolean portrait = directed.preset().motion == com.ji.afkcinematic.cinematic.director.CompositionPreset.Motion.PORTRAIT;
        return client.player.getPosition(partialTick).add(0,
                client.player.getEyeHeight(client.player.getPose()) * (portrait ? .94 : .78), 0);
    }

    public static CameraFrame getFrame() { return FRAMES.current(); }
    public static Vec3 getFramePos() {
        CameraFrame frame = getFrame();
        return new Vec3(frame.x(), frame.y(), frame.z());
    }
    public static float getFramePitch() { return getFrame().pitch(); }
    public static float getFrameYaw() { return getFrame().yaw(); }
    public static float getFrameRoll() { return getFrame().roll(); }
    public static CompositionDirector.Selection getDirectorSelection() { return selection; }
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
                : selection != null && selection.fallback() ? "fallback/safe_anchor"
                : ACTIVE_SEQUENCE.isEmpty() ? "none"
                    : activeShot != null ? activeShot.getId() : ACTIVE_SEQUENCE.get(currentShotIndex).getId();
    }
}
