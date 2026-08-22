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
    }
}
