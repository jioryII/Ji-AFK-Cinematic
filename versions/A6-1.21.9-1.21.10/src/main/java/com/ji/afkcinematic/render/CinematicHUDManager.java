package com.ji.afkcinematic.render;

import com.ji.afkcinematic.config.ModConfig;
import com.ji.afkcinematic.config.ConfigManager;
import net.minecraft.client.MinecraftClient;

/**
 * Specialized manager for HUD visibility and letterbox effects during cinematic mode.
 * Also forces vanilla hideGui=true so Xaero's, Jade, and other HUD-尊重 mods
 * follow F1 semantics during the cinematic.
 */
public class CinematicHUDManager {
    private static boolean captured = false;
    private static boolean previousHideGui = false;

    public static void activate(ModConfig config) {
        if (config.enableLetterbox) {
            LetterboxRenderer.fadeIn();
        }
        HUDController.setHidden(true);

        MinecraftClient client = MinecraftClient.getInstance();
        if (!captured) {
            previousHideGui = client.options.hudHidden;
            captured = true;
        }
        client.options.hudHidden = true;
    }

    public static void deactivate() {
        LetterboxRenderer.reset();
        HUDController.setHidden(false);

        if (captured) {
            MinecraftClient.getInstance().options.hudHidden = previousHideGui;
            captured = false;
        }
    }

    public static void forceRestore() {
        if (captured) {
            MinecraftClient.getInstance().options.hudHidden = previousHideGui;
            captured = false;
        }
    }

    public static boolean isHUDHidden() {
        return HUDController.isHidden();
    }
}
