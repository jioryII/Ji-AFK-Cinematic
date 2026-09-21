package com.ji.afkcinematic.render;

import com.ji.afkcinematic.cinematic.CinematicManager;
import com.ji.afkcinematic.cinematic.CinematicState;
import com.ji.afkcinematic.config.ConfigManager;
import com.ji.afkcinematic.config.SleepLetterboxMode;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;

/** Keeps sleep bars synchronized with the player, even when a sleep mod replaces the GUI. */
public final class SleepLetterboxManager {
    private SleepLetterboxManager() {}

    public static void init() {
        ClientTickEvents.END_CLIENT_TICK.register(SleepLetterboxManager::tick);
    }

    private static void tick(MinecraftClient client) {
        boolean sleeping = client.player != null && client.player.isSleeping();
        LetterboxRenderer.setSleepActive(sleeping && ConfigManager.getConfig().modEnabled
                && ConfigManager.getConfig().sleepLetterboxMode != SleepLetterboxMode.DISABLED);
        if (!sleeping && CinematicManager.getState() != CinematicState.IDLE
                && CinematicManager.isSleepCinematic()) {
            CinematicManager.finishSleepNaturally();
        }
    }
}
