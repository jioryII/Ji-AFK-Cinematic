package com.ji.afkcinematic;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

public class ScreenHelper {
    public static Screen getCurrentScreen(Minecraft client) {
        try {
            // Attempt 26.2 method first
            return client.gui.screen();
        } catch (NoSuchMethodError | NoSuchFieldError e) {
            // Fallback to 26.1 by reflecting over fields to find the one of type Screen
            for (java.lang.reflect.Field f : client.getClass().getFields()) {
                if (f.getType() == Screen.class) {
                    try {
                        return (Screen) f.get(client);
                    } catch (Exception ex) {
                        break;
                    }
                }
            }
        }
        return null;
    }

    public static void setScreen(Minecraft client, Screen screen) {
        try {
            // Attempt 26.2 method first
            client.setScreenAndShow(screen);
        } catch (NoSuchMethodError e) {
            // Fallback to 26.1 by reflecting over methods that take a single Screen parameter
            for (java.lang.reflect.Method m : client.getClass().getMethods()) {
                if (m.getParameterCount() == 1 && m.getParameterTypes()[0] == Screen.class) {
                    try {
                        m.invoke(client, screen);
                        return;
                    } catch (Exception ex) {
                        // ignore and try next
                    }
                }
            }
        }
    }
}
