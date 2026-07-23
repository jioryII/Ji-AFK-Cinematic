package com.ji.afkcinematic.render;

import com.ji.afkcinematic.config.ModConfig;
import net.minecraft.client.MinecraftClient;

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
        LetterboxRenderer.fadeOut();
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
