package com.ji.afkcinematic.input;

import com.ji.afkcinematic.config.PersistentCinematicMode;

/** Pure decision table used by version-specific keyboard and mouse mixins. */
public final class CinematicInputPolicy {
    public enum Event {
        CHAT_OPEN,
        CHAT_INPUT,
        LOOK,
        MOVE,
        JUMP_SNEAK,
        GAMEPLAY_ACTION,
        ESCAPE
    }

    private CinematicInputPolicy() {}

    /** A dedicated sprint press while stationary is not movement intent. */
    public static boolean isStationarySprintOnly(boolean sprintBindingMatches,
                                                  boolean anotherBindingMatches,
                                                  boolean movementHeld,
                                                  double horizontalSpeedSquared) {
        return sprintBindingMatches && !anotherBindingMatches && !movementHeld
                && Double.isFinite(horizontalSpeedSquared)
                && horizontalSpeedSquared >= 0.0 && horizontalSpeedSquared < 1.0e-6;
    }

    public static boolean shouldProcessModShortcuts(boolean chatOpen,
                                                    boolean cinematicActive,
                                                    PersistentCinematicMode mode) {
        return !chatOpen;
    }

    public static boolean shouldRegisterActivity(boolean cinematicActive,
                                                 PersistentCinematicMode mode,
                                                 boolean chatOpen,
                                                 Event event) {
        return shouldRegisterActivity(cinematicActive, false, mode, chatOpen, event);
    }

    /**
     * Fishing cinematics deliberately keep mouse look and gameplay clicks live
     * without treating them as an AFK exit. Only actual movement and Escape
     * end this kind of session; lifecycle and damage exits are handled by the
     * cinematic manager.
     */
    public static boolean shouldRegisterActivity(boolean cinematicActive,
                                                 boolean fishingCinematic,
                                                 PersistentCinematicMode mode,
                                                 boolean chatOpen,
                                                 Event event) {
        if (chatOpen || event == Event.CHAT_OPEN || event == Event.CHAT_INPUT) return false;
        if (cinematicActive && fishingCinematic) {
            return event == Event.MOVE || event == Event.ESCAPE;
        }
        if (event == Event.ESCAPE) return true;
        if (!cinematicActive) return true;
        if (mode == null || mode == PersistentCinematicMode.NORMAL) return true;
        // Once locked, input is deliberately ignored. Strong lifecycle and safety
        // exits (damage, death and disconnect) are handled by CinematicManager.
        if (mode == PersistentCinematicMode.PERSISTENT) return false;
        return true;
    }
}
