package com.ji.afkcinematic;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

public class ScreenHelper {
    public static boolean hasOverlay(Minecraft client) { return client.getOverlay()!=null; }
    public static Screen getCurrentScreen(Minecraft client) {
        return client.screen;
    }

    public static void setScreen(Minecraft client, Screen screen) {
        client.setScreen(screen);
    }
}
