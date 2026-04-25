package com.ji.afkcinematic.afk;

import com.ji.afkcinematic.cinematic.CinematicManager;
import com.ji.afkcinematic.cinematic.CinematicState;
import com.ji.afkcinematic.config.ConfigManager;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;

public class AFKDetector {
    private static int ticksSinceLastActivity = 0;
    private static boolean isLockedOut = false; // Prevents cinematic from restarting infinite loops

    public static void init() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> tick());
    }

    private static void tick() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) {
            ticksSinceLastActivity = 0;
            if (CinematicManager.getState() != CinematicState.IDLE) {
                CinematicManager.reset();
            }
            return;
        }

        if (client.isPaused() || isLockedOut) {
            return;
        }

        CinematicState currentState = CinematicManager.getState();

        if (currentState == CinematicState.IDLE) {
            ticksSinceLastActivity++;
            int threshold = ConfigManager.getConfig().afkThresholdTicks;
            if (ticksSinceLastActivity >= threshold) {
                CinematicManager.onAFKDetected();
            }
        }
    }

    /**
     * Resets activity timer. Removes lockout. Deactivates cinematic.
     */
    public static void registerActivity() {
        ticksSinceLastActivity = 0;
        isLockedOut = false; // Re-enable AFK potential

        CinematicState state = CinematicManager.getState();
        if (state == CinematicState.CINEMATIC_ACTIVE || state == CinematicState.AFK_DETECTED) {
            CinematicManager.deactivateCinematic();
        } else if (state == CinematicState.COMPLETED) {
            CinematicManager.setState(CinematicState.IDLE);
        }
    }

    public static void setLockedOut(boolean lockedOut) {
        isLockedOut = lockedOut;
    }
}
