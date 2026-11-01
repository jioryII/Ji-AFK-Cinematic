package com.ji.afkcinematic.cinematic;

import net.minecraft.client.Minecraft;
import net.minecraft.client.CameraType;

public class CinematicCameraManager {
    private static CameraType previousCameraType;
    private static boolean active;

    public static void activate() {
        if (active) return;
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;

        previousCameraType = client.options.getCameraType();
        active = true;
        client.options.setCameraType(CameraType.THIRD_PERSON_BACK);
    }

    public static void deactivate() {
        if (!active) return;
        Minecraft client = Minecraft.getInstance();
        CameraType restore = previousCameraType;
        previousCameraType = null;
        active = false;
        if (restore != null) {
            client.options.setCameraType(restore);
            com.ji.afkcinematic.compat.SmoothF5Compatibility.prepareImmediateReturn(
                    restore != CameraType.FIRST_PERSON,
                    restore == CameraType.THIRD_PERSON_FRONT);
        }
    }
}
