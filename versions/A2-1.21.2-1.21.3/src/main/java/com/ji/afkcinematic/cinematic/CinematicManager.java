package com.ji.afkcinematic.cinematic;

import com.ji.afkcinematic.afk.AFKDetector;
import com.ji.afkcinematic.afk.AFKListener;
import com.ji.afkcinematic.config.ConfigManager;
import com.ji.afkcinematic.config.DamageAction;
import com.ji.afkcinematic.config.ModConfig;
import com.ji.afkcinematic.music.CinematicMusicManager;
import com.ji.afkcinematic.render.CinematicHUDManager;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.GameMenuScreen;

/**
 * Main orchestrator for the cinematic experience.
 * Orchestrates HUD, Camera, Sound, and State.
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
        if (state == CinematicState.CINEMATIC_ACTIVE || state == CinematicState.AFK_DETECTED) {
            deactivateCinematic();
        } else if (state == CinematicState.COMPLETED) {
            state = CinematicState.IDLE;
        }
    }

    @Override
    public void onReset() {
        if (state != CinematicState.IDLE) {
            reset();
        }
    }

    private static void tick() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null || client.isPaused()) {
            return;
        }

        if (state == CinematicState.CINEMATIC_ACTIVE) {
            handleCinematicTick(client);
        }
    }

    private static void handleCinematicTick(MinecraftClient client) {
        // Check for damage
        if (client.player.hurtTime > 0) {
            ModConfig config = ConfigManager.getConfig();
            DamageAction dAction = config.damageAction;
            if (dAction == DamageAction.CANCEL_CINEMATIC || dAction == DamageAction.PAUSE_GAME) {
                deactivateCinematic();
                AFKDetector.setLockedOut(true);
                
                if (dAction == DamageAction.PAUSE_GAME) {
                    client.setScreen(new GameMenuScreen(true));
                }
                return;
            }
        }

        cinematicTicks++;
        ticksLeftInCurrentShot--;

        // Check for shot boundary
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
        
        // Reset counters
        cinematicTicks = 0;
        currentCycle = 0;
        currentShotIndex = 0;
        ticksLeftInCurrentShot = config.shotDurationTicks;

        // Reset subsystems
        ShotRandomizer.reset();
        CameraController.reset();
        CameraController.startShot(0);

        // Activate managers
        CinematicHUDManager.activate(config);
        CinematicCameraManager.activate();

        if (config.enableMusic) {
            CinematicMusicManager.checkAndPlayMusic();
        }
    }

    public static void deactivateCinematic() {
        if (state != CinematicState.CINEMATIC_ACTIVE) return;
        
        CinematicMusicManager.stopMusic();
        reset();
    }

    public static void reset() {
        state = CinematicState.IDLE;
        CinematicHUDManager.deactivate();
        CinematicCameraManager.deactivate();
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
