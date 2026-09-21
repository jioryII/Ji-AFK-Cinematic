package com.ji.afkcinematic.cinematic;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.Perspective;

public class CinematicCameraManager {
    private static Perspective previousPerspective;
    private static boolean active;

    public static void activate() {
        if (active) return;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        previousPerspective = client.options.getPerspective();
        active = true;
        
        // Use third person back, keeping the player as the camera entity
        // This ensures the player is rendered correctly and chunks load normally
        client.options.setPerspective(Perspective.THIRD_PERSON_BACK);
    }

    public static void deactivate() {
        if (!active) return;
        MinecraftClient client = MinecraftClient.getInstance();
        Perspective restore = previousPerspective;
        previousPerspective = null;
        active = false;
        if (restore != null) {
            client.options.setPerspective(restore);
            com.ji.afkcinematic.compat.SmoothF5Compatibility.prepareImmediateReturn(
                    restore != Perspective.FIRST_PERSON,
                    restore == Perspective.THIRD_PERSON_FRONT);
        }
    }
}
