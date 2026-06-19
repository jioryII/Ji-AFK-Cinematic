package com.ji.afkcinematic.render;

import com.ji.afkcinematic.config.ModConfig;
import com.ji.afkcinematic.config.ConfigManager;

/**
 * Specialized manager for HUD visibility and letterbox effects during cinematic mode.
 */
public class CinematicHUDManager {

    public static void activate(ModConfig config) {
        if (config.enableLetterbox) {
            LetterboxRenderer.fadeIn();
        }
        HUDController.setHidden(true);
    }

    public static void deactivate() {
        LetterboxRenderer.reset();
        HUDController.setHidden(false);
    }

    public static boolean isHUDHidden() {
        return HUDController.isHidden();
    }
}
