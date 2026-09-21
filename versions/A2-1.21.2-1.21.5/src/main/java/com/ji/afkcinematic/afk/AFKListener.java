package com.ji.afkcinematic.afk;

/**
 * Interface for listening to AFK detection events.
 */
public interface AFKListener {
    /** Called when the player has been AFK long enough to trigger cinematic. */
    void onAFKTriggered();

    /** Called when activity is detected, interrupting any potential AFK state. */
    void onActivityDetected();

    /** Called when the detector is reset (e.g. world change). */
    void onReset();
}
