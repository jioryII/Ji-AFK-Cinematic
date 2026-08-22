package com.ji.afkcinematic.input;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.lwjgl.glfw.GLFW;

import static org.junit.jupiter.api.Assertions.*;

class KeySequenceTrackerTest {

    @BeforeEach
    void resetState() {
        KeySequenceTracker.resetAll();
        KeySequenceTracker.resetRebind();
    }

    @Test
    void acceptsEveryDocumentedGlfwKeyIncludingDeleteAndRightArrow() {
        assertTrue(KeySequenceTracker.isBindableKeyCode(GLFW.GLFW_KEY_SPACE));
        assertTrue(KeySequenceTracker.isBindableKeyCode(GLFW.GLFW_KEY_F7));
        assertTrue(KeySequenceTracker.isBindableKeyCode(GLFW.GLFW_KEY_DELETE));
        assertTrue(KeySequenceTracker.isBindableKeyCode(GLFW.GLFW_KEY_RIGHT));
        assertTrue(KeySequenceTracker.isBindableKeyCode(GLFW.GLFW_KEY_MENU));
    }

    @Test
    void rejectsUnknownAndOutOfRangeCodes() {
        assertFalse(KeySequenceTracker.isBindableKeyCode(GLFW.GLFW_KEY_UNKNOWN));
        assertFalse(KeySequenceTracker.isBindableKeyCode(GLFW.GLFW_KEY_SPACE - 1));
        assertFalse(KeySequenceTracker.isBindableKeyCode(GLFW.GLFW_KEY_LAST + 1));
        assertFalse(KeySequenceTracker.isBindableKeyCode(Integer.MAX_VALUE));
    }

    @Test
    void validChordMatchesWhileFirstKeyRemainsPressed() {
        int[] first = KeySequenceTracker.acceptedFirstKeys(GLFW.GLFW_KEY_F7);
        assertFalse(KeySequenceTracker.checkMenu(GLFW.GLFW_KEY_F7, first, GLFW.GLFW_KEY_H));
        assertTrue(KeySequenceTracker.checkMenu(GLFW.GLFW_KEY_H, first, GLFW.GLFW_KEY_H));
    }

    @Test
    void releasingFirstKeyCancelsChord() {
        int[] first = KeySequenceTracker.acceptedFirstKeys(GLFW.GLFW_KEY_F7);
        assertFalse(KeySequenceTracker.checkMenu(GLFW.GLFW_KEY_F7, first, GLFW.GLFW_KEY_H));

        KeySequenceTracker.onKeyReleased(GLFW.GLFW_KEY_F7);

        assertFalse(KeySequenceTracker.checkMenu(GLFW.GLFW_KEY_H, first, GLFW.GLFW_KEY_H));
    }

    @Test
    void releasingUnrelatedKeyDoesNotCancelChord() {
        int[] first = KeySequenceTracker.acceptedFirstKeys(GLFW.GLFW_KEY_F7);
        KeySequenceTracker.checkMenu(GLFW.GLFW_KEY_F7, first, GLFW.GLFW_KEY_H);

        KeySequenceTracker.onKeyReleased(GLFW.GLFW_KEY_G);

        assertTrue(KeySequenceTracker.checkMenu(GLFW.GLFW_KEY_H, first, GLFW.GLFW_KEY_H));
    }

    @Test
    void unknownReleaseCancelsAllPendingChords() {
        int[] menuFirst = KeySequenceTracker.acceptedFirstKeys(GLFW.GLFW_KEY_F7);
        int[] toggleFirst = KeySequenceTracker.acceptedFirstKeys(GLFW.GLFW_KEY_LEFT_CONTROL);
        KeySequenceTracker.checkMenu(GLFW.GLFW_KEY_F7, menuFirst, GLFW.GLFW_KEY_H);
        KeySequenceTracker.checkToggle(GLFW.GLFW_KEY_LEFT_CONTROL, toggleFirst, GLFW.GLFW_KEY_H);

        KeySequenceTracker.onKeyReleased(GLFW.GLFW_KEY_UNKNOWN);

        assertFalse(KeySequenceTracker.checkMenu(GLFW.GLFW_KEY_H, menuFirst, GLFW.GLFW_KEY_H));
        assertFalse(KeySequenceTracker.checkToggle(GLFW.GLFW_KEY_H, toggleFirst, GLFW.GLFW_KEY_H));
    }

    @Test
    void unsupportedRebindKeyIsRejectedWithoutBeingConsumed() {
        int[] out = new int[2];

        assertEquals(
            KeySequenceTracker.UNSUPPORTED_KEY_REJECTED,
            KeySequenceTracker.processRebindKey(GLFW.GLFW_KEY_UNKNOWN, out)
        );
        assertFalse(KeySequenceTracker.hasRebindFirst());

        assertEquals(1, KeySequenceTracker.processRebindKey(GLFW.GLFW_KEY_F7, out));
        assertEquals(
            KeySequenceTracker.UNSUPPORTED_KEY_REJECTED,
            KeySequenceTracker.processRebindKey(GLFW.GLFW_KEY_LAST + 1, out)
        );
        assertTrue(KeySequenceTracker.hasRebindFirst());
        assertEquals(GLFW.GLFW_KEY_F7, KeySequenceTracker.getRebindFirst());
    }

    @Test
    void validRebindCapturesBothKeys() {
        int[] out = new int[2];

        assertEquals(1, KeySequenceTracker.processRebindKey(GLFW.GLFW_KEY_F7, out));
        assertEquals(2, KeySequenceTracker.processRebindKey(GLFW.GLFW_KEY_H, out));
        assertArrayEquals(new int[]{GLFW.GLFW_KEY_F7, GLFW.GLFW_KEY_H}, out);
    }

    @Test
    void escapeCancelsRebind() {
        int[] out = new int[2];
        assertEquals(-1, KeySequenceTracker.processRebindKey(GLFW.GLFW_KEY_ESCAPE, out));
        assertFalse(KeySequenceTracker.hasRebindFirst());
    }
}
