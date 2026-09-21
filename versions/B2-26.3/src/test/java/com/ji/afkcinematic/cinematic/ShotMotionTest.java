package com.ji.afkcinematic.cinematic;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ShotMotionTest {
    @Test
    void equalElapsedTimeProducesEqualMotionAcrossDifferentDurations() {
        float elapsedInFiveSecondShot = ShotMotion.elapsedSeconds(0.4f, 5);
        float elapsedInThirtySecondShot = ShotMotion.elapsedSeconds(2.0f / 30.0f, 30);
        assertEquals(2.0f, elapsedInFiveSecondShot, 0.0001f);
        assertEquals(elapsedInFiveSecondShot, elapsedInThirtySecondShot, 0.0001f);
        assertEquals(
                ShotMotion.phase(elapsedInFiveSecondShot, 0.5f, 12.0f),
                ShotMotion.phase(elapsedInThirtySecondShot, 0.5f, 12.0f),
                0.0001f);
    }

    @Test
    void speedControlsMotionRateInsteadOfDuration() {
        assertEquals(0.25f, ShotMotion.phase(6.0f, 0.5f, 12.0f), 0.0001f);
        assertEquals(0.5f, ShotMotion.phase(6.0f, 1.0f, 12.0f), 0.0001f);
    }

    @Test
    void orbitalTravelContinuesAfterFramingHasSettled() {
        assertEquals(1.5f, ShotMotion.travel(36.0f, 0.5f, 12.0f), 0.0001f);
        assertEquals(1.0f, ShotMotion.phase(36.0f, 0.5f, 12.0f), 0.0001f);
    }
}
