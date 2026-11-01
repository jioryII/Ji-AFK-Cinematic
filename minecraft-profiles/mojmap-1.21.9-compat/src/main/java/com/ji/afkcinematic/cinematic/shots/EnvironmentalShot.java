package com.ji.afkcinematic.cinematic.shots;

import com.ji.afkcinematic.cinematic.DirectedCameraShot;
import com.ji.afkcinematic.cinematic.director.CompositionPreset;

/** Native names remain stable; the shared catalog owns their geometry and identity. */
public final class EnvironmentalShot extends DirectedCameraShot {
    public enum Preset {
        HORIZON_REVEAL,
        GROUND_SKIM,
        CANOPY_GLIDE,
        DISTANT_VISTA,
        PARALLAX_TRUCK,
        SKYWARD_TILT,
        VALLEY_CRANE,
        WIDE_ESTABLISHING,
        HORIZON_ARC,
        GRAND_LANDSCAPE,
        RIVERLINE_DRIFT,
        RIDGELINE_SWEEP,
        CAVE_MOUTH_REVEAL,
        CLOUDLINE_ASCENT,
        FOREGROUND_REVEAL;

        final String id;
        final float sweep;
        Preset() {
            CompositionPreset shared = CompositionPreset.valueOf(name());
            id = name().toLowerCase(java.util.Locale.ROOT);
            sweep = (float) shared.sweep;
        }
    }
    public EnvironmentalShot(Preset preset) { super(CompositionPreset.valueOf(preset.name())); }
}
