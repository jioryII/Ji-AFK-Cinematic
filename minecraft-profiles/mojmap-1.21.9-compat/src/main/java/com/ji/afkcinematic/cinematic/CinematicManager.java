package com.ji.afkcinematic.cinematic;

import com.ji.afkcinematic.ScreenHelper;
import com.ji.afkcinematic.afk.AFKDetector;
import com.ji.afkcinematic.session.CinematicSession;
import com.ji.afkcinematic.session.CinematicSession.Reason;
import com.ji.afkcinematic.session.CinematicSession.Type;
import com.ji.afkcinematic.session.RestorationSteps;
import com.ji.afkcinematic.afk.AFKListener;
import com.ji.afkcinematic.config.ConfigManager;
import com.ji.afkcinematic.config.DamageAction;
import com.ji.afkcinematic.config.ModConfig;
import com.ji.afkcinematic.config.PersistentCinematicMode;
import com.ji.afkcinematic.input.FishingBiteAccess;
import com.ji.afkcinematic.music.CinematicMusicManager;
import com.ji.afkcinematic.render.CinematicHUDManager;
import com.ji.afkcinematic.render.LetterboxRenderer;
import com.ji.afkcinematic.platform.ClientEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.phys.Vec3;

/** Main orchestrator for cinematic sessions and vanilla-aware fishing focus. */
public class CinematicManager implements AFKListener {
    private static final CinematicManager INSTANCE = new CinematicManager();
    private static final int APPROACH_SIGNAL_GRACE_TICKS = 12;
    private static final int POST_BITE_TICKS = 100;
    private static final int POST_CAST_RESUME_TICKS = 60;

    private static boolean sleepCinematic;
    private static final SessionEdit EDIT = new SessionEdit();
    private static final FishingFocusState FISH = new FishingFocusState();
    private static FishingHook observedHook;

    public static void init() {
        CameraController.init();
        AFKDetector.addListener(INSTANCE);
        ClientEvents.END_CLIENT_TICK.register(client -> tick());
    }

    @Override public void onAFKTriggered() { onAFKDetected(); }

    @Override public void onActivityDetected() {
        if (getState() != CinematicState.IDLE) forceDeactivate(AFKDetector.getActivityReason());
    }

    @Override public void onReset() {
        fullTeardown(AFKDetector.getResetReason());
    }

    /** Immediate cancellation used by movement, damage, Esc and manual toggles. */
    public static void forceDeactivate() { forceDeactivate(Reason.USER_REQUEST); }

    private static void forceDeactivate(Reason reason) {
        if (FISH.cinematic) FISH.blocked = true;
        FISH.startTicks = -1;
        stopCinematic(false, false, reason, false);
    }

    /** Screen requests have priority even in Persistent mode; mandatory screens always open. */
    public static void onPresentationOpened(Reason reason) {
        if (getState() != CinematicState.IDLE) stopCinematic(false, false, reason, false);
        AFKDetector.session().holdForPresentation();
        FISH.blocked = false;
        FISH.startTicks = Minecraft.getInstance().player != null && Minecraft.getInstance().player.fishing != null
                && ConfigManager.getConfig().modEnabled && ConfigManager.getConfig().fishingCinematicEnabled
                ? Math.max(1, ConfigManager.getConfig().fishingCinematicThresholdTicks) : -1;
    }
    public static void failForCompatibility() {
        stopCinematic(false, true, Reason.CAMERA_CONFLICT, true);
    }

    /** Natural sleep completion keeps the original bar exit animation. */
    public static void finishSleepNaturally() {
        if (getState() != CinematicState.IDLE) stopCinematic(true, false, Reason.SLEEP, false);
    }

    /** The first pause request is consumed, including controller routes through Gui. */
    public static boolean cancelForPause() {
        if (getState() == CinematicState.IDLE) return false;
        if (FISH.cinematic) FISH.blocked = true;
        FISH.startTicks = -1;
        stopCinematic(false, false, Reason.ESCAPE, true);
        return true;
    }

    public static boolean isFishingCinematic() {
        return getState() == CinematicState.CINEMATIC_ACTIVE && FISH.cinematic;
    }

    public static boolean isFishingBiteActive() { return isFishingCinematic() && FISH.bite; }
    public static int getBiteTicks() { return FISH.focusTicks; }

    public static void toggleImmediate() {
        if (ConfigManager.isLoading()) return;
        AFKDetector.synchronizeContext();
        if (getState() != CinematicState.IDLE) {
            forceDeactivate();
            return;
        }
        Minecraft client = Minecraft.getInstance();
        if (client.player != null && client.level != null) {
            AFKDetector.setLockedOut(false);
            boolean fishingAware = client.player.fishing != null
                    && ConfigManager.getConfig().fishingCinematicEnabled;
            startCinematic(fishingAware, Type.MANUAL);
        } else AFKDetector.session().rejectStart(Type.MANUAL, Reason.NO_WORLD);
    }

    /** Receives the real vanilla FISHING particle that represents an approaching fish. */
    public static void onFishingApproachParticle(Vec3 particlePosition) {
        Minecraft client = Minecraft.getInstance();
        if (getState() != CinematicState.CINEMATIC_ACTIVE || !FISH.cinematic
                || !ConfigManager.getConfig().fishingCinematicEnabled
                || client.player == null || client.player.fishing == null) return;

        Vec3 hook = client.player.fishing.position();
        // Vanilla's approach path is 2-8 blocks from the hook. The lower bound
        // rejects the near-hook splash burst that occurs when the bite starts.
        if (!FishingApproach.belongsToHook(hook, particlePosition)) return;

        FISH.sinceApproach = 0;
        if (!FISH.focus) {
            FISH.focus = true;
            FISH.bite = false;
            FISH.seen = false;
            FISH.focusTicks = 0;
            FISH.postBite = 0;
            CameraController.beginFishingFocus(particlePosition);
        }
    }

    private static void tick() {
        Minecraft client = Minecraft.getInstance();
        if (getState() != CinematicState.IDLE && ScreenHelper.getCurrentScreen(client) instanceof PauseScreen) {
            forceDeactivate(Reason.ESCAPE);
            return;
        }
        if (client.player == null || client.level == null) {
            observedHook = null;
            FISH.blocked = false;
            FISH.startTicks = -1;
            if (getState() != CinematicState.IDLE) fullTeardown(Reason.DISCONNECT);
            return;
        }

        var obstruction = com.ji.afkcinematic.diagnostic.RuntimeResilience.obstruction();
        if (obstruction != Reason.NONE) { onPresentationOpened(obstruction); return; }

        FishingHook hook = client.player.fishing;
        if (hook != observedHook) {
            observedHook = hook;
            if (hook != null) {
                FISH.blocked = false;
                if (getState() == CinematicState.IDLE && ConfigManager.getConfig().modEnabled
                        && ConfigManager.getConfig().fishingCinematicEnabled) {
                    FISH.startTicks = ConfigManager.getConfig().fishingCinematicThresholdTicks;
                    if (FISH.startTicks == 0) startCinematic(true, Type.FISHING);
                } else if (getState() == CinematicState.CINEMATIC_ACTIVE
                        && ConfigManager.getConfig().fishingCinematicEnabled) {
                    FISH.cinematic = true;
                    AFKDetector.session().enterFishing();
                    FISH.startTicks = -1;
                    if (FISH.focus) {
                        // Keep the completed fishing composition briefly after
                        // the next cast, then resume with a fresh normal shot.
                        FISH.postCast = POST_CAST_RESUME_TICKS;
                    }
                }
            } else {
                FISH.startTicks = -1;
            }
        }

        if (client.isPaused()) return;
        if (FISH.startTicks > 0 && hook != null && getState() == CinematicState.IDLE
                && --FISH.startTicks == 0 && !FISH.blocked) {
            startCinematic(true, Type.FISHING);
        }

        if (FISH.postCast > 0 && getState() == CinematicState.CINEMATIC_ACTIVE
                && --FISH.postCast == 0) {
            discardFishingFocus();
        }

        if (getState() == CinematicState.CINEMATIC_ACTIVE) {
            if (FISH.postCast <= 0) updateFishingFocus(hook);
            handleCinematicTick(client);
        }
    }

    private static void updateFishingFocus(FishingHook hook) {
        if (FISH.update(hook instanceof FishingBiteAccess access && access.jiAfk$isBiting())) finishFishingFocus();
    }

    private static void handleCinematicTick(Minecraft client) {
        if (client.player.isDeadOrDying()) {
            stopCinematic(false, false, Reason.DEATH, true);
            return;
        }

        ModConfig config = ConfigManager.getConfig();
        boolean tookDamage = client.player.hurtTime > 0;
        boolean lockedMode = config.persistentMode == PersistentCinematicMode.PERSISTENT;
        if (tookDamage && (lockedMode || config.damageAction != DamageAction.IGNORE)) {
            if (FISH.cinematic) FISH.blocked = true;
            DamageAction action = lockedMode ? DamageAction.CANCEL_CINEMATIC : config.damageAction;
            stopCinematic(false, false, Reason.DAMAGE, true);
            if (action == DamageAction.PAUSE_GAME) ScreenHelper.setScreen(client, new PauseScreen(true));
            return;
        }

        EDIT.ticks++;
        if (FISH.focus) return;
        if (--EDIT.remaining <= 0) advanceShot();
    }

    private static void advanceShot() {
        int totalShots = CameraController.getShotCount();
        if (totalShots <= 0) return;
        ModConfig config = ConfigManager.getConfig();
        SessionEdit.Cut cut = EDIT.next(totalShots, config.maxCycles);
        if (cut == SessionEdit.Cut.LIMIT) {
            if (FISH.cinematic) FISH.blocked = true;
            stopCinematic(true, false, Reason.CYCLE_LIMIT, true); return;
        }
        if (cut == SessionEdit.Cut.CYCLE) CameraController.prepareNextCycle(config.characterShotPercentage);
        CameraController.startShot(EDIT.index);
        EDIT.remaining = getShotDurationTicks();
    }

    private static void finishFishingFocus() {
        FISH.focus = false;
        FISH.bite = false;
        FISH.seen = false;
        FISH.focusTicks = 0;
        FISH.postBite = 0;
        FISH.postCast = 0;
        FISH.sinceApproach = 0;
        CameraController.setBiteActive(false);
        // The interrupted shot is discarded. Continue the same session and cycle
        // budget with a fresh full-length shot; music remains untouched.
        advanceShot();
    }

    private static void discardFishingFocus() {
        if (!FISH.focus) return;
        finishFishingFocus();
    }

    public static void onAFKDetected() {
        if (ConfigManager.isLoading()) return;
        if (getState() != CinematicState.IDLE) return;
        Minecraft client = Minecraft.getInstance();
        if (client.player != null && client.player.fishing != null
                && ConfigManager.getConfig().fishingCinematicEnabled) return;
        startCinematic(false, Type.AFK);
    }

    private static void startCinematic(boolean fishing, Type requestedType) {
        AFKDetector.synchronizeContext();
        ModConfig config = ConfigManager.getConfig();
        boolean sleeping = Minecraft.getInstance().player != null && Minecraft.getInstance().player.isSleeping();
        Type type = sleeping ? Type.SLEEP : fishing ? Type.FISHING : requestedType;
        Reason obstruction = com.ji.afkcinematic.diagnostic.RuntimeResilience.obstruction();
        if (obstruction != Reason.NONE) { AFKDetector.session().rejectStart(type, obstruction); return; }
        var hooks = com.ji.afkcinematic.diagnostic.HookCapabilities.snapshot();
        for (var critical : java.util.List.of(com.ji.afkcinematic.diagnostic.DiagnosticReport.Hook.CAMERA,
                com.ji.afkcinematic.diagnostic.DiagnosticReport.Hook.INPUT,
                com.ji.afkcinematic.diagnostic.DiagnosticReport.Hook.LOOK)) {
            if (hooks.get(critical) == com.ji.afkcinematic.diagnostic.DiagnosticReport.HookStatus.missing) {
                AFKDetector.session().rejectStart(type, Reason.HOOK_MISSING);
                com.ji.afkcinematic.diagnostic.RuntimeResilience.record(com.ji.afkcinematic.diagnostic.DiagnosticIncident.Code.HOOK_MISSING);
                return;
            }
        }
        if (!AFKDetector.session().start(type, config.persistentMode)) return;
        com.ji.afkcinematic.input.RuntimeLookInput.rebase();
        FISH.cinematic = fishing;
        sleepCinematic = sleeping;
        FISH.focus = false;
        FISH.bite = false;
        FISH.seen = false;
        FISH.focusTicks = 0;
        FISH.postBite = 0;
        FISH.postCast = 0;
        FISH.sinceApproach = 0;
        EDIT.ticks = 0;
        EDIT.cycle = 0;
        EDIT.index = 0;
        EDIT.remaining = getShotDurationTicks();

        try {
            ShotRandomizer.reset();
            CameraController.reset();
            CameraController.prepareSequence(config.characterShotPercentage);
            CameraController.startShot(0);
            CinematicHUDManager.activate(config);
            CinematicCameraManager.activate();
            if (config.enableMusic) CinematicMusicManager.checkAndPlayMusic();
        } catch (RuntimeException failure) {
            // Restore all acquired resources; a recoverable adapter error must not crash the tick loop.
            stopCinematic(false, true, Reason.ADAPTER_FAILURE, true);
            com.ji.afkcinematic.diagnostic.RuntimeResilience.record(com.ji.afkcinematic.diagnostic.DiagnosticIncident.Code.ADAPTER_FAILURE);
            com.ji.afkcinematic.JiAFKCinematic.LOGGER.error("Cinematic startup adapter failed; resources restored or recovery blocked", failure);
        } catch (Error failure) {
            try { stopCinematic(false, true, Reason.ADAPTER_FAILURE, true); }
            catch (RuntimeException | Error restoreFailure) {
                if (restoreFailure != failure) failure.addSuppressed(restoreFailure);
            }
            throw failure;
        }
    }

    /** Public compatibility entry point: an explicit call is an immediate cancellation. */
    public static void deactivateCinematic() { stopCinematic(false, false); }

    private static void stopCinematic(boolean natural, boolean resetAllBars) {
        stopCinematic(natural, resetAllBars, Reason.USER_REQUEST, false);
    }

    private static void stopCinematic(boolean natural, boolean resetAllBars, Reason reason, boolean blocked) {
        CinematicSession session = AFKDetector.session();
        CinematicSession.Restoration token = session.beginRestoration(reason, blocked);
        if (token == null && !resetAllBars) return;
        boolean restored = false;
        try {
            RestorationSteps.run(
                    () -> { if (resetAllBars) CinematicMusicManager.forceStop(); else CinematicMusicManager.stopMusic(); },
                    CinematicHUDManager::deactivate,
                    CinematicCameraManager::deactivate,
                    () -> { if (resetAllBars) LetterboxRenderer.reset(); else if (!natural) LetterboxRenderer.cancelCinematic(); },
                    () -> CameraController.setBiteActive(false));
            restored = true;
        } catch (RuntimeException failure) {
            // RestorationSteps already attempted every owner. Complete the token as failed and keep the client alive.
            com.ji.afkcinematic.JiAFKCinematic.LOGGER.error("Cinematic restoration failed after attempting all resources", failure);
        } finally {
            FISH.cinematic = false;
            FISH.postCast = 0;
            sleepCinematic = false;
            FISH.focus = false;
            FISH.bite = false;
            FISH.seen = false;
            FISH.focusTicks = 0;
            FISH.postBite = 0;
            FISH.sinceApproach = 0;
            session.completeRestoration(token, restored);
            if (!restored) com.ji.afkcinematic.diagnostic.RuntimeResilience.record(com.ji.afkcinematic.diagnostic.DiagnosticIncident.Code.RESTORATION_FAILED);
        }
    }

    /** World/disconnect/shutdown teardown also clears scheduled fishing even while idle. */
    public static void fullTeardown() { fullTeardown(Reason.SHUTDOWN); }

    private static void fullTeardown(Reason reason) {
        try { stopCinematic(false, true, reason, false); }
        finally {
            observedHook = null;
            FISH.blocked = false;
            FISH.startTicks = -1;
            EDIT.ticks = 0;
            EDIT.cycle = 0;
            EDIT.remaining = 0;
            EDIT.index = 0;
        }
    }

    @Deprecated public static void reset() { fullTeardown(); }

    public static CinematicState getState() {
        return AFKDetector.session().active() ? CinematicState.CINEMATIC_ACTIVE : CinematicState.IDLE;
    }
    @Deprecated public static void setState(CinematicState newState) {
        if (newState == CinematicState.IDLE) forceDeactivate();
        else if (getState() == CinematicState.IDLE) toggleImmediate();
    }
    public static int getTicksLeftInCurrentShot() { return EDIT.remaining; }
    public static int getShotDurationTicks() { return ConfigManager.getConfig().shotDurationTicks; }
    public static boolean isSleepCinematic() { return sleepCinematic; }
}
