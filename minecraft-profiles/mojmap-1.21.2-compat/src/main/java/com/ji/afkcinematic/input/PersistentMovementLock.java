package com.ji.afkcinematic.input;

import com.ji.afkcinematic.cinematic.CinematicManager;
import com.ji.afkcinematic.cinematic.CinematicState;
import com.ji.afkcinematic.config.ConfigManager;
import com.ji.afkcinematic.config.PersistentCinematicMode;
import net.minecraft.client.player.ClientInput;
import net.minecraft.world.entity.player.Input;

/** Clears only the processed movement fields of the exact 1.21.2 input API. */
public final class PersistentMovementLock {
    private PersistentMovementLock() {}

    public static boolean isLocked() {
        return CinematicManager.getState() == CinematicState.CINEMATIC_ACTIVE
                && ConfigManager.getConfig().persistentMode == PersistentCinematicMode.PERSISTENT;
    }

    public static void clear(ClientInput input) {
        if (!isLocked() || input == null) return;
        input.keyPresses = Input.EMPTY;
        input.leftImpulse = 0.0F;
        input.forwardImpulse = 0.0F;
    }
}
