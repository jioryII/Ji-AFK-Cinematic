package com.ji.afkcinematic.cinematic;

import net.minecraft.client.Minecraft;
import net.minecraft.client.CameraType;

public class CinematicCameraManager {
    private static CameraType previousCameraType;

    public static void activate() {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;
        
        previousCameraType = client.options.getCameraType();
        client.options.setCameraType(CameraType.THIRD_PERSON_BACK);
    }

    public static void deactivate() {
        Minecraft client = Minecraft.getInstance();
        if (previousCameraType != null) {
            client.options.setCameraType(previousCameraType);
        }
    }
}
