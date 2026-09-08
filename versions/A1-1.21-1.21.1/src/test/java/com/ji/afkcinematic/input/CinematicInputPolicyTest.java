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
        assertTrue(CinematicInputPolicy.shouldProcessModShortcuts(
                false, true, PersistentCinematicMode.PERSISTENT));
    }

    @Test
    void classicModeTreatsEveryGameplayInputAsActivity() {
        for (CinematicInputPolicy.Event event : new CinematicInputPolicy.Event[]{LOOK, MOVE, JUMP_SNEAK, GAMEPLAY_ACTION, ESCAPE}) {
            assertTrue(CinematicInputPolicy.shouldRegisterActivity(
                    true, PersistentCinematicMode.NORMAL, false, event));
        }
    }

    @Test
    void interactiveModeIgnoresLookAndChatOpening() {
        assertFalse(activity(true, PersistentCinematicMode.INTERACTIVE, false, LOOK));
        assertFalse(activity(true, PersistentCinematicMode.INTERACTIVE, false, CHAT_OPEN));
        assertFalse(activity(false, PersistentCinematicMode.INTERACTIVE, false, CHAT_OPEN));
    }

    @Test
    void openingChatEndsOnlyANormalActiveCinematic() {
        assertFalse(activity(false, PersistentCinematicMode.NORMAL, false, CHAT_OPEN));
        assertTrue(activity(true, PersistentCinematicMode.NORMAL, false, CHAT_OPEN));
        assertFalse(activity(true, PersistentCinematicMode.INTERACTIVE, false, CHAT_OPEN));
        assertFalse(activity(true, PersistentCinematicMode.PERSISTENT, false, CHAT_OPEN));
    }

    @Test
    void persistentModeLetsIdleThresholdExpireWhileChatIsInUse() {
        for (boolean cinematicActive : new boolean[]{false, true}) {
            assertFalse(activity(cinematicActive, PersistentCinematicMode.INTERACTIVE, true, CHAT_INPUT));
            assertFalse(activity(cinematicActive, PersistentCinematicMode.INTERACTIVE, true, CHAT_OPEN));
            assertFalse(activity(cinematicActive, PersistentCinematicMode.INTERACTIVE, true, LOOK));
            assertFalse(activity(cinematicActive, PersistentCinematicMode.INTERACTIVE, true, GAMEPLAY_ACTION));
            assertTrue(activity(cinematicActive, PersistentCinematicMode.INTERACTIVE, true, ESCAPE));
        }
    }

    @Test
    void gameplayInputStillResetsIdleThresholdBeforeCinematicStarts() {
        assertTrue(activity(false, PersistentCinematicMode.INTERACTIVE, false, LOOK));
        assertTrue(activity(false, PersistentCinematicMode.INTERACTIVE, false, MOVE));
        assertTrue(activity(false, PersistentCinematicMode.INTERACTIVE, false, JUMP_SNEAK));
        assertTrue(activity(false, PersistentCinematicMode.INTERACTIVE, false, GAMEPLAY_ACTION));
    }

    @Test
    void gameplayInputOutsideChatStillCancels() {
        assertTrue(activity(true, PersistentCinematicMode.INTERACTIVE, false, MOVE));
        assertTrue(activity(true, PersistentCinematicMode.INTERACTIVE, false, JUMP_SNEAK));
        assertTrue(activity(true, PersistentCinematicMode.INTERACTIVE, false, GAMEPLAY_ACTION));
    }

    @Test
    void lockedModeOnlyLetsEscapeRegisterAsInputActivity() {
        for (CinematicInputPolicy.Event event : CinematicInputPolicy.Event.values()) {
            assertEquals(event == ESCAPE,
                    activity(true, PersistentCinematicMode.PERSISTENT, false, event));
        }
    }

    private static boolean activity(boolean active, PersistentCinematicMode mode,
                                    boolean chatOpen, CinematicInputPolicy.Event event) {
        return CinematicInputPolicy.shouldRegisterActivity(active, mode, chatOpen, event);
    }
}
