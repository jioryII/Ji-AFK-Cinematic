package com.ji.afkcinematic.cinematic;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.Perspective;
import net.minecraft.entity.Entity;
import com.mojang.authlib.GameProfile;
import java.util.UUID;

public class CinematicCameraManager {
    private static Perspective previousPerspective;

    public static void activate() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;
        
        previousPerspective = client.options.getPerspective();
        
        // Use third person back, keeping the player as the camera entity
        // This ensures the player is rendered correctly and chunks load normally
        client.options.setPerspective(Perspective.THIRD_PERSON_BACK);
    }

    public static void deactivate() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (previousPerspective != null) {
            client.options.setPerspective(previousPerspective);
        }
    }
}
