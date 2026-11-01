package com.ji.afkcinematic;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
/** Typed screen access selected by the exact Minecraft API profile. */
public final class ScreenHelper {
    private ScreenHelper() { }
    public static boolean hasOverlay(Minecraft client) { return client.getOverlay()!=null; }
    public static Screen getCurrentScreen(Minecraft client) { return client.screen; }
    public static void setScreen(Minecraft client, Screen screen) { client.setScreenAndShow(screen); }
}
