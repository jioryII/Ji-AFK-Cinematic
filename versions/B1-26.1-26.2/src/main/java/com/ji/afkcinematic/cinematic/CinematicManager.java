package com.ji.afkcinematic.cinematic;

import com.ji.afkcinematic.afk.AFKDetector;
import com.ji.afkcinematic.afk.AFKListener;
import com.ji.afkcinematic.config.ConfigManager;
import com.ji.afkcinematic.config.DamageAction;
import com.ji.afkcinematic.config.ModConfig;
import com.ji.afkcinematic.config.PersistentCinematicMode;
import com.ji.afkcinematic.music.CinematicMusicManager;
import com.ji.afkcinematic.render.CinematicHUDManager;
import com.ji.afkcinematic.render.LetterboxRenderer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.PauseScreen;

/**
 * Main orchestrator for the cinematic experience (Mojmap).
 */
public class CinematicManager implements AFKListener {
    private static final CinematicManager INSTANCE = new CinematicManager();
    private static CinematicState state = CinematicState.IDLE;

    private static int cinematicTicks = 0;
    private static int currentCycle = 0;
    private static int ticksLeftInCurrentShot = 0;
    private static int currentShotIndex = 0;

    public static void init() {
        CameraController.init();
        AFKDetector.addListener(INSTANCE);
        ClientTickEvents.END_CLIENT_TICK.register(client -> tick());
    }

    @Override
    public void onAFKTriggered() {
        onAFKDetected();
    }

    @Override
    public void onActivityDetected() {
        if (state == CinematicState.CINEMATIC_ACTIVE) {
            deactivateCinematic();
        }
    }

    @Override
    public void onReset() {
        if (state != CinematicState.IDLE) {
            reset();
        }
    }

    public static void forceDeactivate() {
        fullTeardown();
        com.ji.afkcinematic.afk.AFKDetector.setLockedOut(false);
    }

    private static void tick() {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null || client.isPaused()) {
            return;
        }

        if (state == CinematicState.CINEMATIC_ACTIVE) {
            handleCinematicTick(client);
        }
    }

    private static void handleCinematicTick(Minecraft client) {
        // Death/respawn: force immediate teardown (player entity is being recreated)
        if (client.player.isDeadOrDying()) {
            fullTeardown();
            AFKDetector.setLockedOut(true);
            return;
        }

        ModConfig config = ConfigManager.getConfig();
        CinematicHUDManager.updateChatVisibility(config);

        // Direct damage respects the configured action. Predictive fall, fire and
        // low-health cancellation were removed to keep the cinematic behavior explicit.
        boolean tookDamage = client.player.hurtTime > 0;
        boolean lockedMode = config.persistentMode == PersistentCinematicMode.PERSISTENT;
        if (tookDamage && (lockedMode || config.damageAction != DamageAction.IGNORE)) {
            DamageAction dAction = lockedMode ? DamageAction.CANCEL_CINEMATIC : config.damageAction;
            deactivateCinematic();
            AFKDetector.setLockedOut(true);

            if (dAction == DamageAction.PAUSE_GAME) {
                com.ji.afkcinematic.ScreenHelper.setScreen(client, new PauseScreen(true));
            }
            return;
        }

        cinematicTicks++;
        ticksLeftInCurrentShot--;

        if (ticksLeftInCurrentShot <= 0) {
            advanceShot();
        }
    }

    private static void advanceShot() {
        int totalShots = CameraController.getShotCount();
        currentShotIndex = (currentShotIndex + 1) % totalShots;

        if (currentShotIndex == 0 && cinematicTicks > 0) {
            currentCycle++;
            if (currentCycle >= ConfigManager.getConfig().maxCycles) {
                deactivateCinematic();
                AFKDetector.setLockedOut(true);
                return;
            }
        }

        CameraController.startShot(currentShotIndex);
        ticksLeftInCurrentShot = ConfigManager.getConfig().shotDurationTicks;
    }

    public static void onAFKDetected() {
        ModConfig config = ConfigManager.getConfig();
        state = CinematicState.CINEMATIC_ACTIVE;
        
        cinematicTicks = 0;
        currentCycle = 0;
        currentShotIndex = 0;
        ticksLeftInCurrentShot = config.shotDurationTicks;

        ShotRandomizer.reset();
        CameraController.reset();
        CameraController.prepareSequence(config.characterShotPercentage);
        CameraController.startShot(0);

        CinematicHUDManager.activate(config);
        CinematicCameraManager.activate();

        if (config.enableMusic) {
            CinematicMusicManager.checkAndPlayMusic();
        }
    }

    public static void deactivateCinematic() {
        if (state != CinematicState.CINEMATIC_ACTIVE) return;
        CinematicMusicManager.stopMusic();
        CinematicHUDManager.deactivate();
        CinematicCameraManager.deactivate();
        LetterboxRenderer.reset();
        state = CinematicState.IDLE;
    }

    /**
     * Immediate, unconditional teardown. Use on world-change / disconnect / death / shutdown
     * paths where the tick loop cannot be relied on to finish fades. Restores the player's
     * Music sound-category option to its original value (fixes the volume-locked-at-0 leak).
     */
    public static void fullTeardown() {
        CinematicMusicManager.forceStop();
        CinematicHUDManager.deactivate();
        CinematicCameraManager.deactivate();
        LetterboxRenderer.reset();
        state = CinematicState.IDLE;
    }

    /**
     * @deprecated alias kept for call-site compatibility; routes to {@link #fullTeardown()}.
     */
    @Deprecated
    public static void reset() {
        fullTeardown();
    }

    public static CinematicState getState() {
        return state;
    }

    public static void setState(CinematicState newState) {
        state = newState;
    }

    public static int getTicksLeftInCurrentShot() {
        return ticksLeftInCurrentShot;
    }

    public static int getShotDurationTicks() {
        return ConfigManager.getConfig().shotDurationTicks;
    }
}
