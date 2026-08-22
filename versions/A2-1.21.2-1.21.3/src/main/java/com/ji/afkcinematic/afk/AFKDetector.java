package com.ji.afkcinematic.afk;

import com.ji.afkcinematic.config.ConfigManager;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import java.util.ArrayList;
import java.util.List;

/**
 * Monitors player activity and notifies listeners when AFK threshold is reached.
 */
public class AFKDetector {
    private static int ticksSinceLastActivity = 0;
    private static boolean isLockedOut = false;
    private static boolean triggered = false; // one-shot guard so >= threshold fires exactly once
    private static final List<AFKListener> LISTENERS = new ArrayList<>();

    public static void init() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> tick());
    }

    public static void addListener(AFKListener listener) {
        if (!LISTENERS.contains(listener)) {
            LISTENERS.add(listener);
        }
    }

    /** Iterate a defensive copy so listeners that (de)register during dispatch don't CME. */
    private static void dispatch(java.util.function.Consumer<AFKListener> action) {
        new ArrayList<>(LISTENERS).forEach(action);
    }

    private static void tick() {
        if (!com.ji.afkcinematic.config.ConfigManager.getConfig().modEnabled) return;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) {
            ticksSinceLastActivity = 0;
            triggered = false;
            dispatch(AFKListener::onReset);
            return;
        }

        if (client.isPaused() || isLockedOut) {
            return;
        }

        ticksSinceLastActivity++;
        int threshold = ConfigManager.getConfig().afkThresholdTicks;
        // Use >= with a one-shot guard: if the threshold is lowered (e.g. config edited
        // mid-idle) while ticksSinceLastActivity is already past the new value, == would
        // never match again and AFK would silently never trigger until next activity.
        if (ticksSinceLastActivity >= threshold && !triggered) {
            triggered = true;
            dispatch(AFKListener::onAFKTriggered);
        }
    }

    /**
     * Resets activity timer. Removes lockout. Notifies activity.
     */
    public static void registerActivity() {
        if (ticksSinceLastActivity == 0 && !isLockedOut && !triggered) return;

        ticksSinceLastActivity = 0;
        isLockedOut = false;
        triggered = false;
        dispatch(AFKListener::onActivityDetected);
    }

    public static void setLockedOut(boolean lockedOut) {
        isLockedOut = lockedOut;
    }
}
