package com.ji.afkcinematic.client;

/** Friendly display label; transport and diagnostic metadata retain the actual build version. */
public final class ReleaseLabel {
    private ReleaseLabel() {}
    public static String display(String version) {
        if (version.endsWith("-beta.1.0")) return version.substring(0, version.length() - "-beta.1.0".length()) + " beta 1.0";
        return version.replaceFirst("-(?:dev|beta)[.-](\\d+)$", " beta-$1");
    }
    public static String websiteIcon(long milliseconds) {
        return ""; // The native renderer always draws the globe.
    }
}
