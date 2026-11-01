package com.ji.afkcinematic.cinematic;

/** Local UI status only; never sends diagnostics or changes an external shader. */
public final class FocusStatus {
    private FocusStatus() {}
    private static volatile String reason = "ready";
    public static String reason() { return reason; }
    public static void set(String value) { reason = value; }
}
