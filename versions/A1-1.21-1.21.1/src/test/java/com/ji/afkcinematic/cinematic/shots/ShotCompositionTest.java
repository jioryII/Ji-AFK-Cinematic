package com.ji.afkcinematic.cinematic.shots;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class ShotCompositionTest {
    @Test
    void bothPoolsContainFifteenMovingCompositions() {
        assertEquals(15, ComposedCharacterShot.Preset.values().length);
        assertEquals(15, EnvironmentalShot.Preset.values().length);
        for (ComposedCharacterShot.Preset preset : ComposedCharacterShot.Preset.values()) {
            assertNotEquals(0.0f, preset.sweep, preset.id + " must keep moving");
        }
        for (EnvironmentalShot.Preset preset : EnvironmentalShot.Preset.values()) {
            assertNotEquals(0.0f, preset.sweep, preset.id + " must keep moving");
        }
    }
}
