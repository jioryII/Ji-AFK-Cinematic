package com.ji.afkcinematic.config;

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
