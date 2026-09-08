package com.ji.afkcinematic.config;

/** Controls whether passive chat messages remain visible over a cinematic. */
public enum CinematicChatVisibility {
    HIDDEN,
    VISIBLE;

    public CinematicChatVisibility next() {
        CinematicChatVisibility[] values = values();
        return values[(ordinal() + 1) % values.length];
    }

    public boolean isVisible(PersistentCinematicMode mode) {
        return this == VISIBLE;
    }
}
