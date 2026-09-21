package com.ji.afkcinematic.input;

import com.mojang.blaze3d.platform.InputConstants;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LegacyGlfwKeyMapperTest {
    @Test
    void migratesDefaultAndCommonSavedShortcutsToSdlScancodes() {
        assertEquals(InputConstants.KEY_F7, LegacyGlfwKeyMapper.migrate(296));
        assertEquals(InputConstants.KEY_H, LegacyGlfwKeyMapper.migrate(72));
        assertEquals(InputConstants.KEY_LCONTROL, LegacyGlfwKeyMapper.migrate(341));
        assertEquals(InputConstants.KEY_ESCAPE, LegacyGlfwKeyMapper.migrate(256));
    }

    @Test
    void preservesDisabledAndRejectsUnknownLegacyValues() {
        assertEquals(-1, LegacyGlfwKeyMapper.migrate(-1));
        assertEquals(-2, LegacyGlfwKeyMapper.migrate(99999));
    }

    @Test
    void migratesCustomPunctuationNavigationNumpadAndFunctionKeys() {
        assertEquals(InputConstants.KEY_SEMICOLON, LegacyGlfwKeyMapper.migrate(59));
        assertEquals(InputConstants.KEY_PAGEUP, LegacyGlfwKeyMapper.migrate(266));
        assertEquals(InputConstants.KEY_F24, LegacyGlfwKeyMapper.migrate(313));
        assertEquals(InputConstants.KEY_NUMPAD7, LegacyGlfwKeyMapper.migrate(327));
        assertEquals(InputConstants.KEY_RALT, LegacyGlfwKeyMapper.migrate(346));
    }

    @Test
    void distinguishesTypicalGlfwAndSdlSavedSets() {
        assertTrue(LegacyGlfwKeyMapper.isSupported(296));
        assertTrue(LegacyGlfwKeyMapper.isSupported(341));
        assertFalse(LegacyGlfwKeyMapper.isSupported(InputConstants.KEY_F7));
        assertFalse(LegacyGlfwKeyMapper.isSupported(InputConstants.KEY_LCONTROL));
    }
}
