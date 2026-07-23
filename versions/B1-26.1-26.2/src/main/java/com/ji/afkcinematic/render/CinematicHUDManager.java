package com.ji.afkcinematic.render;

import com.ji.afkcinematic.config.ModConfig;
import net.minecraft.client.Minecraft;

/**
 * Specialized manager for HUD visibility and letterbox effects (Mojmap 26.x).
 * Note: 26.x removed the options.hideGui field that older versions used.
 * HUD suppression is handled instead by InGameHudMixin via ModifyVariable.
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

    public static void forceRestore() {
        HUDController.setHidden(false);
    }

    public static boolean isHUDHidden() {
        return HUDController.isHidden();
    }
}
