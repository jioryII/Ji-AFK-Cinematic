package com.ji.afkcinematic;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

public class ScreenHelper {
    public static Screen getCurrentScreen(Minecraft client) {
        // 26.2 moved the active screen behind Gui#screen(), while 26.1 keeps it
        // on Minecraft. Avoid linking either member so one binary can load on both.
        Screen guiScreen = findScreenFromNoArgMethod(client.gui);
        if (guiScreen != null) {
            return guiScreen;
        }
        for (Class<?> type = client.getClass(); type != null; type = type.getSuperclass()) {
            for (java.lang.reflect.Field field : type.getDeclaredFields()) {
                if (field.getType() != Screen.class) continue;
                try {
                    field.setAccessible(true);
                    return (Screen) field.get(client);
                } catch (ReflectiveOperationException | RuntimeException ignored) {
                    // Try the next matching field.
                }
            }
        }
        return null;
    }

    private static Screen findScreenFromNoArgMethod(Object owner) {
        if (owner == null) return null;
        for (java.lang.reflect.Method method : owner.getClass().getMethods()) {
            if (method.getParameterCount() != 0 || method.getReturnType() != Screen.class) continue;
            try {
                return (Screen) method.invoke(owner);
            } catch (ReflectiveOperationException | RuntimeException ignored) {
                // Try the next matching method.
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
