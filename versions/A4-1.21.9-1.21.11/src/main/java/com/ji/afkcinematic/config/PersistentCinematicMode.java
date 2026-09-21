package com.ji.afkcinematic.config;

/** Defines how aggressively player input can interrupt an AFK cinematic. */
public enum PersistentCinematicMode {
    NORMAL,
    INTERACTIVE,
    PERSISTENT;

    public PersistentCinematicMode next() {
        PersistentCinematicMode[] modes = values();
        return modes[(ordinal() + 1) % modes.length];
    }
}
