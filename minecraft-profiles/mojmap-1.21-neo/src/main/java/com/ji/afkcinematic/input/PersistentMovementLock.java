package com.ji.afkcinematic.input;

import com.ji.afkcinematic.cinematic.CinematicManager;
import com.ji.afkcinematic.cinematic.CinematicState;
import com.ji.afkcinematic.config.ConfigManager;
import com.ji.afkcinematic.config.PersistentCinematicMode;
import net.minecraft.client.player.Input;

/** Clear only the exact vanilla movement fields, preserving provider metadata. */
public final class PersistentMovementLock {
    private PersistentMovementLock() {}
    public static boolean isLocked() {
        return CinematicManager.getState() == CinematicState.CINEMATIC_ACTIVE
                && ConfigManager.getConfig().persistentMode == PersistentCinematicMode.PERSISTENT;
    }
    public static void clear(Input input) {
        if (!isLocked() || input == null) return;
        input.leftImpulse = 0.0F;
        input.forwardImpulse = 0.0F;
        input.up = false;
        input.down = false;
        input.left = false;
        input.right = false;
        input.jumping = false;
        input.shiftKeyDown = false;
    }
}
