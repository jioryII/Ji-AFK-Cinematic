package com.ji.afkcinematic.input;

import net.minecraft.client.Minecraft;

/** Native context checks; accepted mouse and unknown/provider rotation stay separate. */
public final class RuntimeLookInput {
    private static final LookInputClassifier TRACKER = new LookInputClassifier();
    private static Object previousPlayer;
    private static Object previousWorld;
    private static Object previousScreen;
    private static boolean previousFocus;
    private static boolean previousCapture;
    private static Object turnPlayer;
    private static double beforeYaw, beforePitch;
    private static boolean acceptedTurn;
    private RuntimeLookInput() { }

    /** Called after vanilla has filtered recapture and immediately before its player.turn call. */
    public static void beginVanillaTurn(double dx, double dy) {
        Minecraft client = Minecraft.getInstance();
        turnPlayer = null;
        acceptedTurn = false;
        if (!synchronize(client) || !client.mouseHandler.isMouseGrabbed()
                || com.ji.afkcinematic.ScreenHelper.getCurrentScreen(client) != null) return;
        turnPlayer = client.player;
        beforeYaw = client.player.getYRot();
        beforePitch = client.player.getXRot();
        double sensitivity = client.options.sensitivity().get() * 0.6000000238418579D + 0.20000000298023224D;
        double gain = sensitivity * sensitivity * sensitivity * 0.15D;
        if (client.options.smoothCamera || !client.player.isScoping()) gain *= 8.0D;
        acceptedTurn = TRACKER.acceptedMouse(dx * gain, dy * gain);
        com.ji.afkcinematic.diagnostic.HookCapabilities.observed(
                com.ji.afkcinematic.diagnostic.DiagnosticReport.Hook.LOOK);
    }

    public static boolean endVanillaTurn() {
        Minecraft client = Minecraft.getInstance();
        if (turnPlayer == null || turnPlayer != client.player || previousWorld != client.level) return false;
        TRACKER.excludeVanillaTurn(beforeYaw, beforePitch, client.player.getYRot(), client.player.getXRot());
        turnPlayer = null;
        boolean result = acceptedTurn;
        acceptedTurn = false;
        return result;
    }

    public static boolean acceptWindow(long window) {
        Minecraft client = Minecraft.getInstance();
        return client.isWindowActive() && window == client.getWindow().getWindow();
    }

    public static boolean isGameplayFocused() {
        Minecraft client = Minecraft.getInstance();
        synchronize(client);
        return client.isWindowActive() && com.ji.afkcinematic.ScreenHelper.getCurrentScreen(client) == null;
    }

    public static boolean onCursorMove(long window, double x, double y) {
        if (!acceptWindow(window)) return false;
        Minecraft client = Minecraft.getInstance();
        if (!synchronize(client)) return false;
        // Vanilla owns captured cursor processing, including its first-move filter.
        // A callback that produces no orientation change cannot cancel gameplay.
        return com.ji.afkcinematic.ScreenHelper.getCurrentScreen(client) != null && TRACKER.cursor(x, y);
    }

    public static boolean pollRotation() {
        Minecraft client = Minecraft.getInstance();
        if (!synchronize(client) || !client.isWindowActive() || !client.mouseHandler.isMouseGrabbed()
                || com.ji.afkcinematic.ScreenHelper.getCurrentScreen(client) != null) return false;
        return TRACKER.rotation(client.player.getYRot(), client.player.getXRot());
    }

    public static void rebase() {
        Minecraft client = Minecraft.getInstance();
        if (!synchronize(client)) return;
        TRACKER.rebaseRotation(client.player.getYRot(), client.player.getXRot());
        TRACKER.rebaseCursor(client.mouseHandler.xpos(), client.mouseHandler.ypos());
    }

    private static boolean synchronize(Minecraft client) {
        if (client.player == null || client.level == null) { reset(); return false; }
        Object screen = com.ji.afkcinematic.ScreenHelper.getCurrentScreen(client);
        boolean focused = client.isWindowActive();
        boolean captured = client.mouseHandler.isMouseGrabbed();
        if (previousPlayer != client.player || previousWorld != client.level
                || previousScreen != screen || previousFocus != focused || previousCapture != captured) {
            if (previousPlayer != null) {
                if (previousPlayer != client.player || previousWorld != client.level
                        || previousFocus != focused || previousCapture != captured) RuntimeShortcuts.reset();
                else if (previousScreen != screen) RuntimeShortcuts.cancelPending();
            }
            TRACKER.reset();
            TRACKER.rebaseRotation(client.player.getYRot(), client.player.getXRot());
            TRACKER.rebaseCursor(client.mouseHandler.xpos(), client.mouseHandler.ypos());
            previousPlayer = client.player;
            previousWorld = client.level;
            previousScreen = screen;
            previousFocus = focused;
            previousCapture = captured;
        }
        return focused;
    }

    public static void reset() {
        TRACKER.reset();
        turnPlayer = null;
        acceptedTurn = false;
        previousPlayer = null;
        previousWorld = null;
        previousScreen = null;
        previousFocus = false;
        previousCapture = false;
    }
}
