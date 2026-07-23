package com.ji.afkcinematic.render;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.components.toasts.ToastManager;
import net.minecraft.network.chat.Component;

public final class ToggleToastManager {
    private static final long DISPLAY_MS = 2000L;

    private ToggleToastManager() {}

    public static void show(boolean enabled) {
        Minecraft client = Minecraft.getInstance();
        if (client == null) return;
        ToastManager manager = getToastManager(client);
        if (manager == null) return;

        ChatFormatting color = enabled ? ChatFormatting.GREEN : ChatFormatting.RED;
        String key = enabled ? "on" : "off";
        Component title = Component.translatable("overlay.ji_afkcinematic.toggle." + key)
            .withStyle(color);

        // Use a fresh SystemToastId each call so consecutive toggles stack as
        // separate toasts (one per slot) instead of rewriting the same slot.
        manager.addToast(new SystemToast(
            new SystemToast.SystemToastId(DISPLAY_MS),
            title,
            null
        ));
    }

    private static ToastManager getToastManager(Minecraft client) {
        // 26.1.x: Minecraft#getToastManager() is a method.
        // 26.2:    Gui#toastManager is a field (Gui was renamed/restructured).
        // 26.2.1+: Gui#toastManager accessor may also be a method again.
        // Try direct method first (26.1.x path), then fall back to field/method
        // on Gui (26.2 path) for robustness.

        // 1) Direct on Minecraft: client.getToastManager()  (26.1.x)
        try {
            java.lang.reflect.Method m = Minecraft.class.getMethod("getToastManager");
            Object tm = m.invoke(client);
            if (tm instanceof ToastManager) return (ToastManager) tm;
        } catch (NoSuchMethodException ignored) {
            // not 26.1.x, fall through
        } catch (Exception e) {
            // try next strategy
        }

        // 2) Field/Method on Gui (26.2 path)
        Object gui;
        try {
            gui = client.gui; // 26.1 field + 26.2 field
        } catch (Exception e) {
            return null;
        }
        if (gui == null) return null;

        // 2a) Method getToastManager() on Gui
        try {
            java.lang.reflect.Method m = gui.getClass().getMethod("getToastManager");
            Object tm = m.invoke(gui);
            if (tm instanceof ToastManager) return (ToastManager) tm;
        } catch (NoSuchMethodException ignored) {
            // try field
        } catch (Exception e) {
            // try field
        }

        // 2b) Field toastManager on Gui
        try {
            for (java.lang.reflect.Field f : gui.getClass().getDeclaredFields()) {
                if (f.getType() == ToastManager.class) {
                    f.setAccessible(true);
                    return (ToastManager) f.get(gui);
                }
            }
        } catch (Exception e) {
            return null;
        }
        return null;
    }
}