package com.ji.afkcinematic.render;

import com.ji.afkcinematic.config.ModConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;

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
        LetterboxRenderer.fadeOut();
        HUDController.setHidden(false);
    }

    public static void updateChatVisibility(ModConfig config) {
        // 26.x does not expose options.hideGui; InGameHudMixin consults the
        // current screen directly, so no mutable option is required here.
    }

    public static boolean isPersistentChatOpen(ModConfig config) {
        return config.persistentMode != com.ji.afkcinematic.config.PersistentCinematicMode.NORMAL
                && com.ji.afkcinematic.ScreenHelper.getCurrentScreen(Minecraft.getInstance()) instanceof ChatScreen;
    }

    public static void forceRestore() {
        HUDController.setHidden(false);
    }

    public static boolean isHUDHidden() {
        return HUDController.isHidden();
    }
}
