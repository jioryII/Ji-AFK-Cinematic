package com.ji.afkcinematic.client;

/** Slow corner animation: each symbol stays readable for three seconds. */
public final class SupportCornerIcon {
    private SupportCornerIcon() {}
    public static String at(long milliseconds) {
        return switch ((int)Math.floorMod(milliseconds / 3000, 3)) {
            case 0 -> "§e⚠";
            case 1 -> "§b♦";
            default -> "§a✉";
        };
    }
}
