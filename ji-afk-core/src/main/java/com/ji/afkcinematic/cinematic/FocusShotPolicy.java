package com.ji.afkcinematic.cinematic;

import com.ji.afkcinematic.cinematic.director.CompositionPreset;

/** Close character views use optical depth; other compositions use edge softness. */
public final class FocusShotPolicy {
    private FocusShotPolicy() {}
    public static boolean usesDepth(CompositionPreset preset, boolean fishing) {
        return !fishing && preset != null && preset.character && preset.range() == CompositionPreset.Range.CLOSE;
    }
}
