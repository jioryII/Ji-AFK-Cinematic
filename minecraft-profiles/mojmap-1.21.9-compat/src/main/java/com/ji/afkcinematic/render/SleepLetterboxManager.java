package com.ji.afkcinematic.render;

import com.ji.afkcinematic.cinematic.CinematicManager;
import com.ji.afkcinematic.cinematic.CinematicState;
import com.ji.afkcinematic.config.ConfigManager;
import com.ji.afkcinematic.config.SleepLetterboxMode;
import com.ji.afkcinematic.platform.ClientEvents;
import net.minecraft.client.Minecraft;

/** A sleep overlay driven by vanilla player state, independent of sleep mods. */
public final class SleepLetterboxManager {
    private static boolean sleeping;

    private SleepLetterboxManager() {}

    public static void init() { ClientEvents.END_CLIENT_TICK.register(SleepLetterboxManager::tick); }

    private static void tick(Minecraft client) {
        boolean nowSleeping = client.player != null && client.player.isSleeping();
        boolean showBars = nowSleeping && ConfigManager.getConfig().modEnabled
                && ConfigManager.getConfig().sleepLetterboxMode != SleepLetterboxMode.DISABLED;
        sleeping = nowSleeping;
        // setSleepActive is idempotent and must also observe option changes while still in bed.
        LetterboxRenderer.setSleepActive(showBars);
        if (client.player != null && !nowSleeping && CinematicManager.getState() != CinematicState.IDLE
                && CinematicManager.isSleepCinematic()) {
            CinematicManager.finishSleepNaturally();
        }
    }

    public static boolean isSleeping() { return sleeping; }
}
