package com.ji.afkcinematic.cinematic;

import net.minecraft.util.math.Vec3d;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FishingApproachTest {
    private static final Vec3d PLAYER = new Vec3d(0, 64, 0);
    private static final Vec3d HOOK = new Vec3d(0, 64, 5);

    @Test
    void acceptsVanillaApproachRangeAndRejectsBiteBurstOrOtherHooks() {
        assertTrue(FishingApproach.belongsToHook(HOOK, new Vec3d(2, 64, 5)));
        assertTrue(FishingApproach.belongsToHook(HOOK, new Vec3d(8.5, 67.5, 5)));
        assertFalse(FishingApproach.belongsToHook(HOOK, new Vec3d(0.2, 64, 5)));
        assertFalse(FishingApproach.belongsToHook(HOOK, new Vec3d(9, 64, 5)));
        assertFalse(FishingApproach.belongsToHook(HOOK, new Vec3d(2, 68, 5)));
    }

    @Test
    void cameraIsPlacedOppositeTheObservedApproachDirection() {
        int positiveApproach = FishingApproach.preferredCameraSide(PLAYER, HOOK, new Vec3d(-2, 64, 5));
        int negativeApproach = FishingApproach.preferredCameraSide(PLAYER, HOOK, new Vec3d(2, 64, 5));
        assertEquals(-1, positiveApproach, "camera must use the opposite lateral side");
        assertEquals(1, negativeApproach, "camera must use the opposite lateral side");
        assertEquals(0, FishingApproach.preferredCameraSide(PLAYER, HOOK, new Vec3d(0.1, 64, 4)));
    }
}
