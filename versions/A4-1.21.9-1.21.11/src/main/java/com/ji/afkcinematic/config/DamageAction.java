package com.ji.afkcinematic.config;

// Must be a separate enum to avoid inner class serialization issues easily
public enum DamageAction {
    IGNORE,
    CANCEL_CINEMATIC,
    PAUSE_GAME
}
