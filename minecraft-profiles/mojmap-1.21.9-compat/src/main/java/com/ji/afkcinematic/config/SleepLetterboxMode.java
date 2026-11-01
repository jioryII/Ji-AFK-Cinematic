package com.ji.afkcinematic.config;

public enum SleepLetterboxMode {
    DISABLED, ENABLED,
    /** Read-only compatibility with configurations written before beta-1. */
    @Deprecated MODERATE, @Deprecated COMPLETE;

    public SleepLetterboxMode canonical() { return this == DISABLED ? DISABLED : ENABLED; }
    public SleepLetterboxMode next() { return this == DISABLED ? ENABLED : DISABLED; }
}
