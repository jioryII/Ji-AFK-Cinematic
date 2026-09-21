package com.ji.afkcinematic.render;

import com.ji.afkcinematic.cinematic.CinematicManager;
import com.ji.afkcinematic.cinematic.CinematicState;
import com.ji.afkcinematic.config.ConfigManager;
import com.ji.afkcinematic.config.SleepLetterboxMode;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;

/** A sleep overlay driven by vanilla player state, independent of sleep mods. */
public final class SleepLetterboxManager {
    private static boolean sleeping;

    private SleepLetterboxManager() {}

    public static void init() { ClientTickEvents.END_CLIENT_TICK.register(SleepLetterboxManager::tick); }

    private static void tick(Minecraft client) {
        boolean nowSleeping = client.player != null && client.player.isSleeping()
                && ConfigManager.getConfig().modEnabled
                && ConfigManager.getConfig().sleepLetterboxMode != SleepLetterboxMode.DISABLED;
        if (nowSleeping != sleeping) {
            sleeping = nowSleeping;
            LetterboxRenderer.setSleepActive(sleeping);
        }
        if (!nowSleeping && CinematicManager.getState() != CinematicState.IDLE
                && CinematicManager.isSleepCinematic()) {
            CinematicManager.finishSleepNaturally();
        }
    }

    public static boolean isSleeping() { return sleeping; }
}
