package com.ji.afkcinematic.cinematic;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CameraRotationProfileTest {
    @Test
    void sameSeedProducesStablePerShotAngles() {
        assertArrayEquals(
                CameraRotationProfile.plan(15, 42L),
                CameraRotationProfile.plan(15, 42L));
    }

    @Test
    void disabledRotationAlwaysProducesStableHorizon() {
        assertEquals(0.0f, CameraRotationProfile.apply(0.9f, false));
    }

    @Test
    void selectedAngleRemainsFixedForTheWholeShot() {
        float angle = CameraRotationProfile.plan(15, 123L)[4];
        assertEquals(CameraRotationProfile.apply(angle, true), CameraRotationProfile.apply(angle, true));
    }

    @Test
    void plannedAnglesAreSubtleAndUsuallyLevel() {
        int level = 0;
        float[] angles = CameraRotationProfile.plan(1000, 99L);
        for (float angle : angles) {
            assertTrue(angle >= -0.9f && angle <= 0.9f);
            if (angle == 0.0f) level++;
        }
        assertTrue(level >= 600, "most shots should keep a level horizon");
    }
}
