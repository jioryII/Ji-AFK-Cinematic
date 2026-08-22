package com.ji.afkcinematic.input;

import com.ji.afkcinematic.config.PersistentCinematicMode;

/** Pure decision table used by version-specific keyboard and mouse mixins. */
public final class CinematicInputPolicy {
    public enum Event {
        KEY_PRESS,
        OPEN_CHAT_KEY,
        ESCAPE_KEY,
        MOUSE_MOVE,
        MOUSE_CLICK,
        MOUSE_SCROLL
    }

    private CinematicInputPolicy() {}

    public static boolean shouldProcessModShortcuts(boolean chatOpen,
                                                    boolean cinematicActive,
                                                    PersistentCinematicMode mode) {
        return !chatOpen && !(cinematicActive && mode == PersistentCinematicMode.PERSISTENT);
    }

    public static boolean shouldRegisterActivity(boolean cinematicActive,
                                                 PersistentCinematicMode mode,
                                                 boolean chatOpen,
                                                 Event event) {
        if (mode == null || mode == PersistentCinematicMode.NORMAL) {
            return true;
        }
        if (event == Event.ESCAPE_KEY) {
            return true;
        }
        // Once locked, input is deliberately ignored. Strong lifecycle and safety
        // exits (damage, death and disconnect) are handled by CinematicManager.
        if (cinematicActive && mode == PersistentCinematicMode.PERSISTENT) {
            return false;
        }
        // The chat-opening key arrives before Minecraft installs ChatScreen. Ignore it
        // explicitly so opening chat cannot postpone the AFK threshold.
        if (event == Event.OPEN_CHAT_KEY) {
            return false;
        }
        // Chat remains usable without resetting the idle clock, both before and during
        // a persistent cinematic. Escape is handled above because it intentionally exits.
        if (chatOpen) {
            return false;
        }
        if (!cinematicActive) {
            return true;
        }
        return event != Event.MOUSE_MOVE;
    }
}
