package com.ji.afkcinematic.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PersistentCinematicModeTest {
    @Test
    void buttonCycleFollowsNormalInteractivePersistentOrder() {
        assertEquals(PersistentCinematicMode.INTERACTIVE, PersistentCinematicMode.NORMAL.next());
        assertEquals(PersistentCinematicMode.PERSISTENT, PersistentCinematicMode.INTERACTIVE.next());
        assertEquals(PersistentCinematicMode.NORMAL, PersistentCinematicMode.PERSISTENT.next());
    }
}
