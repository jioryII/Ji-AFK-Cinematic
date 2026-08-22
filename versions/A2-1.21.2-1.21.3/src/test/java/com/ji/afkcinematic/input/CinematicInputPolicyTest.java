package com.ji.afkcinematic.input;

import org.junit.jupiter.api.Test;
import com.ji.afkcinematic.config.PersistentCinematicMode;
import static com.ji.afkcinematic.input.CinematicInputPolicy.Event.*;
import static org.junit.jupiter.api.Assertions.*;

class CinematicInputPolicyTest {
    @Test
    void chatOwnsKeyboardInputInsteadOfTriggeringModShortcuts() {
        assertFalse(CinematicInputPolicy.shouldProcessModShortcuts(
                true, false, PersistentCinematicMode.NORMAL));
        assertTrue(CinematicInputPolicy.shouldProcessModShortcuts(
                false, false, PersistentCinematicMode.NORMAL));
        assertFalse(CinematicInputPolicy.shouldProcessModShortcuts(
                false, true, PersistentCinematicMode.PERSISTENT));
    }

    @Test
    void classicModeTreatsEveryInputAsActivity() {
        for (CinematicInputPolicy.Event event : CinematicInputPolicy.Event.values()) {
            assertTrue(CinematicInputPolicy.shouldRegisterActivity(
                    true, PersistentCinematicMode.NORMAL, false, event));
        }
    }

    @Test
    void persistentModeIgnoresPassiveMouseAndChatOpening() {
        assertFalse(activity(true, PersistentCinematicMode.INTERACTIVE, false, MOUSE_MOVE));
        assertFalse(activity(true, PersistentCinematicMode.INTERACTIVE, false, OPEN_CHAT_KEY));
        assertFalse(activity(false, PersistentCinematicMode.INTERACTIVE, false, OPEN_CHAT_KEY));
    }

    @Test
    void persistentModeLetsIdleThresholdExpireWhileChatIsInUse() {
        for (boolean cinematicActive : new boolean[]{false, true}) {
            assertFalse(activity(cinematicActive, PersistentCinematicMode.INTERACTIVE, true, KEY_PRESS));
            assertFalse(activity(cinematicActive, PersistentCinematicMode.INTERACTIVE, true, OPEN_CHAT_KEY));
            assertFalse(activity(cinematicActive, PersistentCinematicMode.INTERACTIVE, true, MOUSE_MOVE));
            assertFalse(activity(cinematicActive, PersistentCinematicMode.INTERACTIVE, true, MOUSE_CLICK));
            assertFalse(activity(cinematicActive, PersistentCinematicMode.INTERACTIVE, true, MOUSE_SCROLL));
            assertTrue(activity(cinematicActive, PersistentCinematicMode.INTERACTIVE, true, ESCAPE_KEY));
        }
    }

    @Test
    void gameplayInputStillResetsIdleThresholdBeforeCinematicStarts() {
        assertTrue(activity(false, PersistentCinematicMode.INTERACTIVE, false, KEY_PRESS));
        assertTrue(activity(false, PersistentCinematicMode.INTERACTIVE, false, MOUSE_MOVE));
        assertTrue(activity(false, PersistentCinematicMode.INTERACTIVE, false, MOUSE_CLICK));
        assertTrue(activity(false, PersistentCinematicMode.INTERACTIVE, false, MOUSE_SCROLL));
    }

    @Test
    void gameplayInputOutsideChatStillCancels() {
        assertTrue(activity(true, PersistentCinematicMode.INTERACTIVE, false, KEY_PRESS));
        assertTrue(activity(true, PersistentCinematicMode.INTERACTIVE, false, MOUSE_CLICK));
        assertTrue(activity(true, PersistentCinematicMode.INTERACTIVE, false, MOUSE_SCROLL));
    }

    @Test
    void lockedModeOnlyLetsEscapeRegisterAsInputActivity() {
        for (CinematicInputPolicy.Event event : CinematicInputPolicy.Event.values()) {
            assertEquals(event == ESCAPE_KEY,
                    activity(true, PersistentCinematicMode.PERSISTENT, false, event));
        }
    }

    private static boolean activity(boolean active, PersistentCinematicMode mode,
                                    boolean chatOpen, CinematicInputPolicy.Event event) {
        return CinematicInputPolicy.shouldRegisterActivity(active, mode, chatOpen, event);
    }
}
