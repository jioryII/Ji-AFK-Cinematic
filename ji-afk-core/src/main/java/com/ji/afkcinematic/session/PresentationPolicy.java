package com.ji.afkcinematic.session;

/** A screen's meaning, independent of loader, perspective, cursor capture and rendering. */
public final class PresentationPolicy {
    public enum Screen { GAMEPLAY, CHAT, OWN_SETTINGS, SLEEP, INTERFACE }
    private PresentationPolicy() { }
    public static CinematicSession.Reason obstruction(Screen screen, boolean externalTransition) {
        if (screen == Screen.INTERFACE) return CinematicSession.Reason.SCREEN_OPEN;
        // Intentional Ji presentations retain their existing semantics.
        if (screen == Screen.GAMEPLAY && externalTransition) return CinematicSession.Reason.COMPAT_TRANSITION;
        return CinematicSession.Reason.NONE;
    }
}
