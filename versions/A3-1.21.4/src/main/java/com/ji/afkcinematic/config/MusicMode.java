package com.ji.afkcinematic.config;

public enum MusicMode {
    VANILLA(true, false),
    MIXED(true, true),
    CUSTOM(false, true);

    private final boolean vanilla;
    private final boolean custom;

    MusicMode(boolean vanilla, boolean custom) {
        this.vanilla = vanilla;
        this.custom = custom;
    }

    public boolean includesVanilla() { return vanilla; }
    public boolean includesCustom() { return custom; }

    public MusicMode next() {
        MusicMode[] values = values();
        return values[(ordinal() + 1) % values.length];
    }
}
