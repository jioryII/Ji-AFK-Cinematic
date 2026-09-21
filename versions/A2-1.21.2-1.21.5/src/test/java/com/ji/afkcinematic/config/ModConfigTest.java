package com.ji.afkcinematic.config;

import org.junit.jupiter.api.Test;
import org.lwjgl.glfw.GLFW;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModConfigTest {

    @Test
    void disabledShortcutsRemainDisabledAfterRecalculate() {
        ModConfig config = new ModConfig();
        config.menuKey1 = -1;
        config.menuKey2 = -1;
        config.toggleKey1 = -1;
        config.toggleKey2 = -1;

        config.recalculate();

        assertEquals(-1, config.menuKey1);
        assertEquals(-1, config.menuKey2);
        assertEquals(-1, config.toggleKey1);
        assertEquals(-1, config.toggleKey2);
    }

    @Test
    void deleteAndRightArrowRemainValidBindings() {
        ModConfig config = new ModConfig();
        config.menuKey1 = GLFW.GLFW_KEY_DELETE;
        config.menuKey2 = GLFW.GLFW_KEY_RIGHT;

        config.recalculate();

        assertEquals(GLFW.GLFW_KEY_DELETE, config.menuKey1);
        assertEquals(GLFW.GLFW_KEY_RIGHT, config.menuKey2);
    }

    @Test
    void shotMixIsClampedAndSnappedToTenPercentSteps() {
        ModConfig config = new ModConfig();
        config.characterShotPercentage = 84;
        config.recalculate();
        assertEquals(80, config.characterShotPercentage);

        config.characterShotPercentage = 106;
        config.recalculate();
        assertEquals(100, config.characterShotPercentage);
    }

    @Test
    void removedLegacySettingsUseFixedPolicy() {
        ModConfig config = new ModConfig();
        config.useEasing = false;
        config.easingIntensity = 0.0f;
        config.cancelOnFallDamage = true;
        config.cancelOnFire = true;
        config.lowHealthThreshold = 0.75f;

        config.recalculate();

        assertFalse(config.useEasing);
        assertEquals(0.0f, config.easingIntensity, 0.0001f);
        assertFalse(config.cancelOnFallDamage);
        assertFalse(config.cancelOnFire);
        assertEquals(0.0f, config.lowHealthThreshold, 0.0001f);
    }

    @Test
    void cinematicCompositionDefaultsAreCalmAndEnvironmentFocused() {
        ModConfig config = new ModConfig();
        assertFalse(config.cameraRotationEnabled);
        assertEquals(PersistentCinematicMode.NORMAL, config.persistentMode);
        assertEquals(30, config.characterShotPercentage);
        assertEquals(0.5f, config.cinematicMusicVolume, 0.0001f);
        assertEquals(CinematicChatVisibility.VISIBLE, config.chatVisibility);
        assertEquals(GLFW.GLFW_KEY_LEFT_CONTROL, config.toggleKey1);
        assertEquals(GLFW.GLFW_KEY_H, config.toggleKey2);
        assertEquals(GLFW.GLFW_KEY_F7, config.immediateKey1);
        assertEquals(GLFW.GLFW_KEY_I, config.immediateKey2);
        assertEquals(MusicMode.VANILLA, config.musicMode);
        assertEquals(SleepLetterboxMode.MODERATE, config.sleepLetterboxMode);
        assertTrue(config.fishingCinematicEnabled);
        assertEquals("", config.customMusicDirectory);
    }

    @Test
    void fishingThresholdDefaultsToTenSecondsAndAllowsImmediateStart() {
        ModConfig config = new ModConfig();
        assertEquals(10, config.fishingCinematicThresholdSeconds);
        config.fishingCinematicThresholdSeconds = -4;
        config.recalculate();
        assertEquals(0, config.fishingCinematicThresholdSeconds);
        assertEquals(0, config.fishingCinematicThresholdTicks);
        config.fishingCinematicThresholdSeconds = 999;
        config.recalculate();
        assertEquals(120, config.fishingCinematicThresholdSeconds);
        assertEquals(2400, config.fishingCinematicThresholdTicks);
    }

    @Test
    void nullableSleepAndMusicFolderSettingsAreRepaired() {
        ModConfig config = new ModConfig();
        config.sleepLetterboxMode = null;
        config.customMusicDirectory = null;
        config.recalculate();
        assertEquals(SleepLetterboxMode.MODERATE, config.sleepLetterboxMode);
        assertTrue(config.fishingCinematicEnabled);
        assertEquals("", config.customMusicDirectory);
    }

    @Test
    void sleepLetterboxModeCyclesThroughAllThreeStates() {
        assertEquals(SleepLetterboxMode.MODERATE, SleepLetterboxMode.DISABLED.next());
        assertEquals(SleepLetterboxMode.COMPLETE, SleepLetterboxMode.MODERATE.next());
        assertEquals(SleepLetterboxMode.DISABLED, SleepLetterboxMode.COMPLETE.next());
    }

    @Test
    void unlimitedCyclesSurviveRecalculationAndOtherValuesAreClamped() {
        ModConfig config = new ModConfig();
        config.maxCycles = ModConfig.UNLIMITED_CYCLES;
        config.recalculate();
        assertTrue(config.isUnlimitedCycles());

        config.maxCycles = 0;
        config.recalculate();
        assertEquals(1, config.maxCycles);

        config.maxCycles = 99;
        config.recalculate();
        assertEquals(20, config.maxCycles);
    }

    @Test
    void portableNamesRestoreCustomAndDisabledBindings() {
        ModConfig config = new ModConfig();
        config.menuKey1 = GLFW.GLFW_KEY_DELETE;
        config.menuKey2 = GLFW.GLFW_KEY_RIGHT;
        config.toggleKey1 = -1;
        config.toggleKey2 = -1;
        config.syncPortableKeyNames();

        assertEquals("key.keyboard.delete", config.menuKey1Name);
        assertEquals("key.keyboard.right", config.menuKey2Name);
        assertEquals("disabled", config.toggleKey1Name);

        config.menuKey1 = GLFW.GLFW_KEY_F7;
        config.menuKey2 = GLFW.GLFW_KEY_H;
        config.toggleKey1 = GLFW.GLFW_KEY_LEFT_CONTROL;
        config.toggleKey2 = GLFW.GLFW_KEY_H;
        config.applyPortableKeyNames();

        assertEquals(GLFW.GLFW_KEY_DELETE, config.menuKey1);
        assertEquals(GLFW.GLFW_KEY_RIGHT, config.menuKey2);
        assertEquals(-1, config.toggleKey1);
        assertEquals(-1, config.toggleKey2);
    }

    @Test
    void defaultShortcutNamesRoundTripAcrossInputBackends() {
        ModConfig config = new ModConfig();
        config.syncPortableKeyNames();
        assertEquals("key.keyboard.f7", config.menuKey1Name);
        assertEquals("key.keyboard.left.control", config.toggleKey1Name);
        assertEquals("key.keyboard.i", config.immediateKey2Name);

        config.menuKey1 = -2;
        config.menuKey2 = -2;
        config.toggleKey1 = -2;
        config.toggleKey2 = -2;
        config.immediateKey1 = -2;
        config.immediateKey2 = -2;
        config.applyPortableKeyNames();

        assertEquals(GLFW.GLFW_KEY_F7, config.menuKey1);
        assertEquals(GLFW.GLFW_KEY_H, config.menuKey2);
        assertEquals(GLFW.GLFW_KEY_LEFT_CONTROL, config.toggleKey1);
        assertEquals(GLFW.GLFW_KEY_H, config.toggleKey2);
        assertEquals(GLFW.GLFW_KEY_F7, config.immediateKey1);
        assertEquals(GLFW.GLFW_KEY_I, config.immediateKey2);
    }}
