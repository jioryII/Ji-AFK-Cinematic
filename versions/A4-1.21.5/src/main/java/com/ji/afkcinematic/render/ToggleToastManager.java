package com.ji.afkcinematic.render;

import net.minecraft.util.Formatting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.toast.SystemToast;
import net.minecraft.client.toast.ToastManager;
import net.minecraft.text.Text;

public final class ToggleToastManager {
    private static final long DISPLAY_MS = 2000L;
    private static final SystemToast.Type OUR_TYPE = new SystemToast.Type(DISPLAY_MS);

    private ToggleToastManager() {}

    public static void show(boolean enabled) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) return;
        ToastManager manager = getToastManager(client);
        if (manager == null) return;

        Formatting color = enabled ? Formatting.GREEN : Formatting.RED;
        String key = enabled ? "on" : "off";
        Text title = Text.translatable("overlay.ji_afkcinematic.toggle." + key)
            .copy()
            .formatted(color);

        SystemToast.add(manager, OUR_TYPE, title, null);
    }

    private static ToastManager getToastManager(MinecraftClient client) {
        try {
            return client.getToastManager();
        } catch (NoSuchMethodError | NoSuchFieldError e) {
            return null;
        }
    }
}
