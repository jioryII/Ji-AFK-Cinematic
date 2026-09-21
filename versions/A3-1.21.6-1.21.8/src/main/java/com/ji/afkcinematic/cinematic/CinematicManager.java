package com.ji.afkcinematic.cinematic;

import com.ji.afkcinematic.afk.AFKDetector;
import com.ji.afkcinematic.afk.AFKListener;
import com.ji.afkcinematic.config.ConfigManager;
import com.ji.afkcinematic.config.DamageAction;
import com.ji.afkcinematic.config.ModConfig;
import com.ji.afkcinematic.config.PersistentCinematicMode;
import com.ji.afkcinematic.input.FishingBiteAccess;
import com.ji.afkcinematic.music.CinematicMusicManager;
import com.ji.afkcinematic.render.CinematicHUDManager;
import com.ji.afkcinematic.render.LetterboxRenderer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.entity.projectile.FishingBobberEntity;
import net.minecraft.util.math.Vec3d;

/** Main orchestrator for cinematic sessions and vanilla-aware fishing focus. */
public class CinematicManager implements AFKListener {
    private static final CinematicManager INSTANCE = new CinematicManager();
    private static final int APPROACH_SIGNAL_GRACE_TICKS = 12;
    private static final int POST_BITE_TICKS = 100;
    private static final int POST_CAST_RESUME_TICKS = 60;

    private static CinematicState state = CinematicState.IDLE;
    private static int cinematicTicks;
    private static int currentCycle;
    private static int ticksLeftInCurrentShot;
    private static int currentShotIndex;
    private static int fishingStartTicks = -1;
    private static int focusTicks;
    private static int ticksSinceApproachSignal;
    private static int postBiteTicks;
    private static int postCastResumeTicks;
    private static boolean biteActive;
    private static boolean biteSeen;
    private static boolean fishingFocusActive;
    private static boolean fishingCinematic;
    private static boolean fishingBlocked;
    private static boolean sleepCinematic;
    private static FishingBobberEntity observedHook;

    public static void init() {
        CameraController.init();
        AFKDetector.addListener(INSTANCE);
        ClientTickEvents.END_CLIENT_TICK.register(client -> tick());
    }

    @Override public void onAFKTriggered() { onAFKDetected(); }

    @Override public void onActivityDetected() {
        if (state != CinematicState.IDLE) forceDeactivate();
    }

    @Override public void onReset() {
        if (state != CinematicState.IDLE) reset();
    }

    /** Immediate cancellation used by movement, damage, Esc and manual toggles. */
    public static void forceDeactivate() {
        if (fishingCinematic) fishingBlocked = true;
        fishingStartTicks = -1;
        stopCinematic(false, false);
        AFKDetector.setLockedOut(false);
    }

    /** Natural sleep completion keeps the original bar exit animation. */
    public static void finishSleepNaturally() {
        if (state != CinematicState.IDLE) stopCinematic(true, false);
    }

    /** The first pause request is consumed, including controller routes through Gui. */
    public static boolean cancelForPause() {
        if (state == CinematicState.IDLE) return false;
        if (fishingCinematic) fishingBlocked = true;
        fishingStartTicks = -1;
        stopCinematic(false, false);
        AFKDetector.setLockedOut(true);
        return true;
    }

    public static boolean isFishingCinematic() {
        return state == CinematicState.CINEMATIC_ACTIVE && fishingCinematic;
    }

    public static boolean isFishingBiteActive() { return isFishingCinematic() && biteActive; }
    public static int getBiteTicks() { return focusTicks; }

    public static void toggleImmediate() {
        if (state != CinematicState.IDLE) {
            forceDeactivate();
            return;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null && client.world != null) {
            AFKDetector.setLockedOut(false);
            boolean fishingAware = client.player.fishHook != null
                    && ConfigManager.getConfig().fishingCinematicEnabled;
            startCinematic(fishingAware);
        }
    }

    /** Receives the real vanilla FISHING particle that represents an approaching fish. */
    public static void onFishingApproachParticle(Vec3d particlePosition) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (state != CinematicState.CINEMATIC_ACTIVE || !fishingCinematic
                || !ConfigManager.getConfig().fishingCinematicEnabled
                || client.player == null || client.player.fishHook == null) return;

        Vec3d hook = client.player.fishHook.getPos();
        // Vanilla's approach path is 2-8 blocks from the hook. The lower bound
        // rejects the near-hook splash burst that occurs when the bite starts.
        if (!FishingApproach.belongsToHook(hook, particlePosition)) return;

        ticksSinceApproachSignal = 0;
        if (!fishingFocusActive) {
            fishingFocusActive = true;
            biteActive = false;
            biteSeen = false;
            focusTicks = 0;
            postBiteTicks = 0;
            CameraController.beginFishingFocus(particlePosition);
        }
    }

    private static void tick() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (state != CinematicState.IDLE && client.currentScreen instanceof GameMenuScreen) {
            forceDeactivate();
            return;
        }
        if (client.player == null || client.world == null) {
            observedHook = null;
            fishingBlocked = false;
            fishingStartTicks = -1;
            if (state != CinematicState.IDLE) fullTeardown();
            return;
        }

        FishingBobberEntity hook = client.player.fishHook;
        if (hook != observedHook) {
            observedHook = hook;
            if (hook != null) {
                fishingBlocked = false;
                if (state == CinematicState.IDLE && ConfigManager.getConfig().modEnabled
                        && ConfigManager.getConfig().fishingCinematicEnabled) {
                    fishingStartTicks = ConfigManager.getConfig().fishingCinematicThresholdTicks;
                    if (fishingStartTicks == 0) startCinematic(true);
                } else if (state == CinematicState.CINEMATIC_ACTIVE
                        && ConfigManager.getConfig().fishingCinematicEnabled) {
                    fishingCinematic = true;
                    fishingStartTicks = -1;
                    if (fishingFocusActive) {
                        // Keep the completed fishing composition briefly after
                        // the next cast, then resume with a fresh normal shot.
                        postCastResumeTicks = POST_CAST_RESUME_TICKS;
                    }
                }
            } else {
                fishingStartTicks = -1;
            }
        }

        if (client.isPaused()) return;
        if (fishingStartTicks > 0 && hook != null && state == CinematicState.IDLE
                && --fishingStartTicks == 0 && !fishingBlocked) {
            startCinematic(true);
        }

        if (postCastResumeTicks > 0 && state == CinematicState.CINEMATIC_ACTIVE
                && --postCastResumeTicks == 0) {
            discardFishingFocus();
        }

        if (state == CinematicState.CINEMATIC_ACTIVE) {
            if (postCastResumeTicks <= 0) updateFishingFocus(hook);
            handleCinematicTick(client);
        }
    }

    private static void updateFishingFocus(FishingBobberEntity hook) {
        if (!fishingFocusActive) return;
        focusTicks++;
        ticksSinceApproachSignal++;
        boolean bitingNow = hook instanceof FishingBiteAccess access && access.jiAfk$isBiting();
        biteActive = bitingNow;
        if (bitingNow) {
            biteSeen = true;
            postBiteTicks = 0;
        } else if (biteSeen) {
            if (++postBiteTicks >= POST_BITE_TICKS) finishFishingFocus();
        } else if (ticksSinceApproachSignal > APPROACH_SIGNAL_GRACE_TICKS) {
            finishFishingFocus();
        }
    }

    private static void handleCinematicTick(MinecraftClient client) {
        if (client.player.deathTime > 0 || client.player.getHealth() <= 0.0F) {
            stopCinematic(false, false);
            AFKDetector.setLockedOut(true);
            return;
        }

        ModConfig config = ConfigManager.getConfig();
        boolean tookDamage = client.player.hurtTime > 0;
        boolean lockedMode = config.persistentMode == PersistentCinematicMode.PERSISTENT;
        if (tookDamage && (lockedMode || config.damageAction != DamageAction.IGNORE)) {
            if (fishingCinematic) fishingBlocked = true;
            DamageAction action = lockedMode ? DamageAction.CANCEL_CINEMATIC : config.damageAction;
            stopCinematic(false, false);
            AFKDetector.setLockedOut(true);
            if (action == DamageAction.PAUSE_GAME) client.setScreen(new GameMenuScreen(true));
            return;
        }

        cinematicTicks++;
        if (fishingFocusActive) return;
        if (--ticksLeftInCurrentShot <= 0) advanceShot();
    }

    private static void advanceShot() {
        int totalShots = CameraController.getShotCount();
        if (totalShots <= 0) return;
        currentShotIndex = (currentShotIndex + 1) % totalShots;
        if (currentShotIndex == 0 && cinematicTicks > 0) {
            currentCycle++;
            ModConfig config = ConfigManager.getConfig();
            if (!config.isUnlimitedCycles() && currentCycle >= config.maxCycles) {
                if (fishingCinematic) fishingBlocked = true;
                stopCinematic(true, false);
                AFKDetector.setLockedOut(true);
                return;
            }
        }
        CameraController.startShot(currentShotIndex);
        ticksLeftInCurrentShot = getShotDurationTicks();
    }

    private static void finishFishingFocus() {
        fishingFocusActive = false;
        biteActive = false;
        biteSeen = false;
        focusTicks = 0;
        postBiteTicks = 0;
        postCastResumeTicks = 0;
        ticksSinceApproachSignal = 0;
        CameraController.setBiteActive(false);
        // The interrupted shot is discarded. Continue the same session and cycle
        // budget with a fresh full-length shot; music remains untouched.
        advanceShot();
    }

    private static void discardFishingFocus() {
        if (!fishingFocusActive) return;
        finishFishingFocus();
    }

    public static void onAFKDetected() {
        if (state != CinematicState.IDLE) return;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null && client.player.fishHook != null
                && ConfigManager.getConfig().fishingCinematicEnabled) return;
        startCinematic(false);
    }

    private static void startCinematic(boolean fishing) {
        ModConfig config = ConfigManager.getConfig();
        if (state != CinematicState.IDLE) stopCinematic(false, false);
        state = CinematicState.CINEMATIC_ACTIVE;
        fishingCinematic = fishing;
        sleepCinematic = MinecraftClient.getInstance().player != null && MinecraftClient.getInstance().player.isSleeping();
        fishingFocusActive = false;
        biteActive = false;
        biteSeen = false;
        focusTicks = 0;
        postBiteTicks = 0;
        postCastResumeTicks = 0;
        ticksSinceApproachSignal = 0;
        cinematicTicks = 0;
        currentCycle = 0;
        currentShotIndex = 0;
        ticksLeftInCurrentShot = getShotDurationTicks();

        ShotRandomizer.reset();
        CameraController.reset();
        CameraController.prepareSequence(config.characterShotPercentage);
        CameraController.startShot(0);
        CinematicHUDManager.activate(config);
        CinematicCameraManager.activate();
        if (config.enableMusic) CinematicMusicManager.checkAndPlayMusic();
    }

    /** Public compatibility entry point: an explicit call is an immediate cancellation. */
    public static void deactivateCinematic() { stopCinematic(false, false); }

    private static void stopCinematic(boolean natural, boolean resetAllBars) {
        if (state == CinematicState.IDLE && !resetAllBars) return;
        // User-facing exits always fade the cinematic track. Only a full
        // world/client teardown needs the immediate safety stop.
        if (resetAllBars) CinematicMusicManager.forceStop();
        else CinematicMusicManager.stopMusic();
        CinematicHUDManager.deactivate();
        CinematicCameraManager.deactivate();
        if (resetAllBars) LetterboxRenderer.reset();
        else if (!natural) LetterboxRenderer.cancelCinematic();
        state = CinematicState.IDLE;
        fishingCinematic = false;
        postCastResumeTicks = 0;
        sleepCinematic = false;
        fishingFocusActive = false;
        biteActive = false;
        biteSeen = false;
        focusTicks = 0;
        postBiteTicks = 0;
        ticksSinceApproachSignal = 0;
        CameraController.setBiteActive(false);
    }

    /** World/disconnect/shutdown teardown, including all independent bar sources. */
    public static void fullTeardown() { stopCinematic(false, true); }

    @Deprecated public static void reset() { fullTeardown(); }

    public static CinematicState getState() { return state; }
    public static void setState(CinematicState newState) { state = newState; }
    public static int getTicksLeftInCurrentShot() { return ticksLeftInCurrentShot; }
    public static int getShotDurationTicks() { return ConfigManager.getConfig().shotDurationTicks; }
    public static boolean isSleepCinematic() { return sleepCinematic; }
}
