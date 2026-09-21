package com.ji.afkcinematic.render;

import com.ji.afkcinematic.JiAFKCinematic;
import net.minecraft.util.Formatting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.toast.SystemToast;
import net.minecraft.client.toast.ToastManager;
import net.minecraft.text.Text;

public final class ToggleToastManager {
    private static final long DISPLAY_MS = 2000L;
    // Lazy-init: a static "new SystemToast.Type(...)" runs at class-load and would throw
    // ExceptionInInitializerError (-> NoClassDefFoundError on subsequent uses) if the
    // Type constructor signature changes in a future MC version. Resolve once, on demand.
    private static volatile SystemToast.Type cachedType;

    private ToggleToastManager() {}

    private static SystemToast.Type getType() {
        if (cachedType != null) return cachedType;
        try {
            cachedType = new SystemToast.Type(DISPLAY_MS);
        } catch (Throwable t) {
            JiAFKCinematic.LOGGER.warn("Could not init SystemToast.Type, toggle toast disabled", t);
        }
        return cachedType;
    }

    public static void show(boolean enabled) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) return;
        ToastManager manager = getToastManager(client);
        if (manager == null) return;
        SystemToast.Type type = getType();
        if (type == null) return;

        Formatting color = enabled ? Formatting.GREEN : Formatting.RED;
        String key = enabled ? "on" : "off";
        Text title = Text.translatable("overlay.ji_afkcinematic.toggle." + key)
            .copy()
            .formatted(color);

        try {
            SystemToast.add(manager, type, title, null);
        } catch (Throwable t) {
            JiAFKCinematic.LOGGER.warn("Could not show toggle toast", t);
        }
    }

    private static ToastManager getToastManager(MinecraftClient client) {
        try {
            return client.getToastManager();
        } catch (NoSuchMethodError | NoSuchFieldError e) {
            return null;
        }
    }
}
