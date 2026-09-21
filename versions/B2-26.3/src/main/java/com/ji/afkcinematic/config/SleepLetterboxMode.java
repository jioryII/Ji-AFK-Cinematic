package com.ji.afkcinematic.config;

public enum SleepLetterboxMode {
    DISABLED,
    MODERATE,
    COMPLETE;

    public SleepLetterboxMode next() {
        SleepLetterboxMode[] values = values();
        return values[(ordinal() + 1) % values.length];
    }
}
