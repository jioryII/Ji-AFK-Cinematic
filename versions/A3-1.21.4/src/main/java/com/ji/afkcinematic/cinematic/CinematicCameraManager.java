package com.ji.afkcinematic.cinematic;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.Perspective;

public class CinematicCameraManager {
    private static Perspective previousPerspective;

    public static void activate() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) return;
        
        previousPerspective = client.options.getPerspective();
        
        // Use third person back, keeping the player as the camera entity visually
        client.options.setPerspective(Perspective.THIRD_PERSON_BACK);
    }

    public static void deactivate() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (previousPerspective != null) {
            client.options.setPerspective(previousPerspective);
        }
    }
}
