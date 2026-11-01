package com.ji.afkcinematic.render;

import com.ji.afkcinematic.config.ModConfig;

/** Owns cinematic visibility; chat is drawn separately from the suppressed HUD. */
public class CinematicHUDManager {

    public static void activate(ModConfig config) {
        if (config.enableLetterbox) {
            LetterboxRenderer.fadeIn();
        }
        HUDController.setHidden(true);
    }

    public static void deactivate() {
        LetterboxRenderer.fadeOut();
        HUDController.setHidden(false);
    }

    public static boolean shouldRenderPassiveChat(ModConfig config) {
        // The chat pass never changes global HUD suppression.
        return config.chatVisibility == com.ji.afkcinematic.config.CinematicChatVisibility.VISIBLE;
    }

    public static void forceRestore() {
        HUDController.setHidden(false);
    }

    public static boolean isHUDHidden() {
        return HUDController.isHidden();
    }
}
