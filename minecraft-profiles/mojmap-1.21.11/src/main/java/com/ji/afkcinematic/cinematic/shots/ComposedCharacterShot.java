package com.ji.afkcinematic.cinematic.shots;

import com.ji.afkcinematic.cinematic.DirectedCameraShot;
import com.ji.afkcinematic.cinematic.director.CompositionPreset;

/** Native names remain stable; the shared catalog owns their geometry and identity. */
public final class ComposedCharacterShot extends DirectedCameraShot {
    public enum Preset {
        EYE_LEVEL_ARC,
        LOW_HERO_ARC,
        HIGH_PORTRAIT,
        LEFT_SHOULDER,
        RIGHT_SHOULDER,
        SLOW_PUSH_IN,
        SLOW_PULL_BACK,
        CRANE_RISE,
        CRANE_DESCENT,
        AERIAL_THREE_QUARTER,
        DISTANT_SILHOUETTE,
        GROUND_TO_PORTRAIT,
        PROFILE_TRUCK,
        TIGHT_PORTRAIT,
        HEROIC_THREE_QUARTER;

        final String id;
        final float sweep;
        Preset() {
            CompositionPreset shared = CompositionPreset.valueOf(name());
            id = name().toLowerCase(java.util.Locale.ROOT);
            sweep = (float) shared.sweep;
        }
    }
    public ComposedCharacterShot(Preset preset) { super(CompositionPreset.valueOf(preset.name())); }
}
