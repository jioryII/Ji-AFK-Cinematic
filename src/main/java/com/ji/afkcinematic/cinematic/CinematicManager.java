package com.ji.afkcinematic.cinematic;

import com.ji.afkcinematic.afk.AFKDetector;
import com.ji.afkcinematic.config.ConfigManager;
import com.ji.afkcinematic.config.DamageAction;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.option.Perspective;
import com.ji.afkcinematic.music.CinematicMusicManager;
import com.ji.afkcinematic.render.HUDController;
import com.ji.afkcinematic.render.LetterboxRenderer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;

public class CinematicManager {
    private static CinematicState state = CinematicState.IDLE;

    private static int cinematicTicks = 0;
    private static int currentCycle = 0;

    private static int ticksLeftInCurrentShot = 0;
    private static int currentShotIndex = 0;

    private static Perspective savedPerspective = Perspective.FIRST_PERSON;

    public static void init() {
        CameraController.init();
        ClientTickEvents.END_CLIENT_TICK.register(client -> tick());
    }

    private static void tick() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null || client.isPaused()) {
            return;
        }

        if (state == CinematicState.CINEMATIC_ACTIVE) {
            // Check for damage
            if (client.player.hurtTime > 0) {
                DamageAction dAction = ConfigManager.getConfig().damageAction;
                if (dAction == DamageAction.CANCEL_CINEMATIC || dAction == DamageAction.PAUSE_GAME) {
                    deactivateCinematic();
                    AFKDetector.setLockedOut(true); // Don't trigger again immediately
                    
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
                int totalShots = CameraController.getShotCount();
                currentShotIndex = (currentShotIndex + 1) % totalShots;
                if (currentShotIndex == 0 && cinematicTicks > 0) {
                    currentCycle++;
                    if (currentCycle >= ConfigManager.getConfig().maxCycles) {
                        deactivateCinematic();
                        AFKDetector.setLockedOut(true); // Stop loop when cinematic reaches its time limit
                        return;
                    }
                }
                CameraController.startShot(currentShotIndex);
                ticksLeftInCurrentShot = ConfigManager.getConfig().shotDurationTicks;
            }
        }
    }

    public static void onAFKDetected() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.options == null) return;

        state = CinematicState.CINEMATIC_ACTIVE;
        cinematicTicks = 0;
        currentCycle = 0;
        currentShotIndex = 0;

        ShotRandomizer.reset();
        CameraController.reset();
        CameraController.startShot(0);
        ticksLeftInCurrentShot = ConfigManager.getConfig().shotDurationTicks;

        if (ConfigManager.getConfig().enableLetterbox) {
            LetterboxRenderer.fadeIn();
        }
        HUDController.setHidden(true);

        if (ConfigManager.getConfig().enableMusic) {
            CinematicMusicManager.checkAndPlayMusic();
        }

        savedPerspective = client.options.getPerspective();
        client.options.setPerspective(Perspective.THIRD_PERSON_BACK);
    }

    public static void deactivateCinematic() {
        if (state != CinematicState.CINEMATIC_ACTIVE) return;
        CinematicMusicManager.stopMusic();
        reset();
    }

    public static CinematicState getState() {
        return state;
    }

    public static void setState(CinematicState newState) {
        state = newState;
    }

    public static void reset() {
        state = CinematicState.IDLE;
        LetterboxRenderer.reset();
        HUDController.setHidden(false);
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null && client.options != null) {
            client.options.smoothCameraEnabled = false;
            client.options.setPerspective(savedPerspective);
        }
    }

    public static int getTicksLeftInCurrentShot() {
        return ticksLeftInCurrentShot;
    }

    public static int getShotDurationTicks() {
        return ConfigManager.getConfig().shotDurationTicks;
    }
}
