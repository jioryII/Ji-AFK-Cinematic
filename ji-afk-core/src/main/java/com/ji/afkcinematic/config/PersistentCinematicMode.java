package com.ji.afkcinematic.config;

/** Defines how aggressively player input can interrupt an AFK cinematic. */
public enum PersistentCinematicMode {
    NORMAL,
    /** Legacy serialized value, never selectable. */
    @Deprecated INTERACTIVE,
    PERSISTENT;

    public PersistentCinematicMode next() {
        return this == PERSISTENT ? NORMAL : PERSISTENT;
    }
}
