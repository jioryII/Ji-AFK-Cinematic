package com.ji.afkcinematic.afk;

import com.ji.afkcinematic.config.ConfigManager;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import java.util.ArrayList;
import java.util.List;

/**
 * Monitors player activity and notifies listeners (Mojmap 26.x).
 */
public class AFKDetector {
    private static int ticksSinceLastActivity = 0;
    private static boolean isLockedOut = false;
    private static final List<AFKListener> LISTENERS = new ArrayList<>();

    public static void init() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> tick());
    }

    public static void addListener(AFKListener listener) {
        if (!LISTENERS.contains(listener)) {
            LISTENERS.add(listener);
        }
    }

    private static void tick() {
        if (!com.ji.afkcinematic.config.ConfigManager.getConfig().modEnabled) return;
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null) {
            ticksSinceLastActivity = 0;
            LISTENERS.forEach(AFKListener::onReset);
            return;
        }

        if (client.isPaused() || isLockedOut) {
            return;
        }

        ticksSinceLastActivity++;
        int threshold = ConfigManager.getConfig().afkThresholdTicks;
        if (ticksSinceLastActivity == threshold) {
            LISTENERS.forEach(AFKListener::onAFKTriggered);
        }
    }

    public static void registerActivity() {
        if (ticksSinceLastActivity == 0 && !isLockedOut) return;

        ticksSinceLastActivity = 0;
        isLockedOut = false;
        LISTENERS.forEach(AFKListener::onActivityDetected);
    }

    public static void setLockedOut(boolean lockedOut) {
        isLockedOut = lockedOut;
    }
}
