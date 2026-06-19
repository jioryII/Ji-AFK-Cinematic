package com.ji.afkcinematic.render;

import com.ji.afkcinematic.config.ModConfig;

/**
 * Specialized manager for HUD visibility and letterbox effects (Mojmap).
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
