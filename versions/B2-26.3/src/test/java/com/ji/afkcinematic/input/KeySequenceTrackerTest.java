package com.ji.afkcinematic.input;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.mojang.blaze3d.platform.InputConstants;

import static org.junit.jupiter.api.Assertions.*;

class KeySequenceTrackerTest {

    @BeforeEach
    void resetState() {
        KeySequenceTracker.resetAll();
        KeySequenceTracker.resetRebind();
    }

    @Test
    void acceptsVanillaSdlScancodesIncludingLettersDeleteAndRightArrow() {
        assertTrue(KeySequenceTracker.isBindableKeyCode(InputConstants.KEY_A));
        assertTrue(KeySequenceTracker.isBindableKeyCode(InputConstants.KEY_SPACE));
        assertTrue(KeySequenceTracker.isBindableKeyCode(InputConstants.KEY_F7));
        assertTrue(KeySequenceTracker.isBindableKeyCode(InputConstants.KEY_DELETE));
        assertTrue(KeySequenceTracker.isBindableKeyCode(InputConstants.KEY_RIGHT));
        assertTrue(KeySequenceTracker.isBindableKeyCode(InputConstants.KEY_F12));
    }

    @Test
    void rejectsUnknownAndOutOfRangeCodes() {
        assertFalse(KeySequenceTracker.isBindableKeyCode(0));
        assertFalse(KeySequenceTracker.isBindableKeyCode(65536));
        assertFalse(KeySequenceTracker.isBindableKeyCode(Integer.MAX_VALUE));
    }

    @Test
    void validChordMatchesWhileFirstKeyRemainsPressed() {
        int[] first = KeySequenceTracker.acceptedFirstKeys(InputConstants.KEY_F7);
        assertFalse(KeySequenceTracker.checkMenu(InputConstants.KEY_F7, first, InputConstants.KEY_H));
        assertTrue(KeySequenceTracker.checkMenu(InputConstants.KEY_H, first, InputConstants.KEY_H));
    }

    @Test
    void menuToggleAndImmediateChordsKeepIndependentState() {
        int[] menu = KeySequenceTracker.acceptedFirstKeys(InputConstants.KEY_F7);
        int[] toggle = KeySequenceTracker.acceptedFirstKeys(InputConstants.KEY_LCONTROL);
        int[] immediate = KeySequenceTracker.acceptedFirstKeys(InputConstants.KEY_F7);

        assertFalse(KeySequenceTracker.checkMenu(InputConstants.KEY_F7, menu, InputConstants.KEY_H));
        assertFalse(KeySequenceTracker.checkImmediate(InputConstants.KEY_F7, immediate, InputConstants.KEY_I));
        assertFalse(KeySequenceTracker.checkToggle(InputConstants.KEY_LCONTROL, toggle, InputConstants.KEY_H));
        assertTrue(KeySequenceTracker.checkToggle(InputConstants.KEY_H, toggle, InputConstants.KEY_H));
        assertTrue(KeySequenceTracker.checkImmediate(InputConstants.KEY_I, immediate, InputConstants.KEY_I));
    }

    @Test
    void releasingFirstKeyCancelsChord() {
        int[] first = KeySequenceTracker.acceptedFirstKeys(InputConstants.KEY_F7);
        assertFalse(KeySequenceTracker.checkMenu(InputConstants.KEY_F7, first, InputConstants.KEY_H));

        KeySequenceTracker.onKeyReleased(InputConstants.KEY_F7);

        assertFalse(KeySequenceTracker.checkMenu(InputConstants.KEY_H, first, InputConstants.KEY_H));
    }

    @Test
    void releasingUnrelatedKeyDoesNotCancelChord() {
        int[] first = KeySequenceTracker.acceptedFirstKeys(InputConstants.KEY_F7);
        KeySequenceTracker.checkMenu(InputConstants.KEY_F7, first, InputConstants.KEY_H);

        KeySequenceTracker.onKeyReleased(InputConstants.KEY_G);

        assertTrue(KeySequenceTracker.checkMenu(InputConstants.KEY_H, first, InputConstants.KEY_H));
    }

    @Test
    void unknownReleaseCancelsAllPendingChords() {
        int[] menuFirst = KeySequenceTracker.acceptedFirstKeys(InputConstants.KEY_F7);
        int[] toggleFirst = KeySequenceTracker.acceptedFirstKeys(InputConstants.KEY_LCONTROL);
        KeySequenceTracker.checkMenu(InputConstants.KEY_F7, menuFirst, InputConstants.KEY_H);
        KeySequenceTracker.checkToggle(InputConstants.KEY_LCONTROL, toggleFirst, InputConstants.KEY_H);

        KeySequenceTracker.onKeyReleased(0);

        assertFalse(KeySequenceTracker.checkMenu(InputConstants.KEY_H, menuFirst, InputConstants.KEY_H));
        assertFalse(KeySequenceTracker.checkToggle(InputConstants.KEY_H, toggleFirst, InputConstants.KEY_H));
    }

    @Test
    void unsupportedRebindKeyIsRejectedWithoutBeingConsumed() {
        int[] out = new int[2];

        assertEquals(
            KeySequenceTracker.UNSUPPORTED_KEY_REJECTED,
            KeySequenceTracker.processRebindKey(0, out)
        );
        assertFalse(KeySequenceTracker.hasRebindFirst());

        assertEquals(1, KeySequenceTracker.processRebindKey(InputConstants.KEY_F7, out));
        assertEquals(
            KeySequenceTracker.UNSUPPORTED_KEY_REJECTED,
            KeySequenceTracker.processRebindKey(65536, out)
        );
        assertTrue(KeySequenceTracker.hasRebindFirst());
        assertEquals(InputConstants.KEY_F7, KeySequenceTracker.getRebindFirst());
    }

    @Test
    void validRebindCapturesBothKeys() {
        int[] out = new int[2];

        assertEquals(1, KeySequenceTracker.processRebindKey(InputConstants.KEY_F7, out));
        assertEquals(2, KeySequenceTracker.processRebindKey(InputConstants.KEY_H, out));
        assertArrayEquals(new int[]{InputConstants.KEY_F7, InputConstants.KEY_H}, out);
    }

    @Test
    void escapeCancelsRebind() {
        int[] out = new int[2];
        assertEquals(-1, KeySequenceTracker.processRebindKey(InputConstants.KEY_ESCAPE, out));
        assertFalse(KeySequenceTracker.hasRebindFirst());
    }
}
