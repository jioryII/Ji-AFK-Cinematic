package com.ji.afkcinematic.afk;

/**
 * Interface for listening to AFK detection events (Mojmap).
 */
public interface AFKListener {
    void onAFKTriggered();
    void onActivityDetected();
    void onReset();
}
