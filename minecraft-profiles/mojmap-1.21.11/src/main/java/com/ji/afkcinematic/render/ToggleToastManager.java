package com.ji.afkcinematic.render;

import com.ji.afkcinematic.JiAFKCinematic;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.components.toasts.ToastManager;
import net.minecraft.network.chat.Component;

public final class ToggleToastManager {
    private static final long DISPLAY_MS = 2000L;

    private ToggleToastManager() {}

    public static void show(boolean enabled) {
        showMessage(Component.translatable("overlay.ji_afkcinematic.toggle." + (enabled ? "on" : "off"))
                .withStyle(enabled ? ChatFormatting.GREEN : ChatFormatting.RED));
    }
    public static void showSaveFailure() {
        showMessage(Component.translatable("config.ji_afkcinematic.save_failed").withStyle(ChatFormatting.RED));
    }
    private static SystemToast menuHint;
    public static void hideMenuHint() { if(menuHint!=null)menuHint.forceHide(); }
    public static void refreshMenuHint(Component shortcut) { if(menuHint!=null)menuHint.reset(Component.literal("Ji AFK Cinematic").withStyle(ChatFormatting.GOLD),Component.translatable("overlay.ji_afkcinematic.menu_hint",shortcut)); }
    public static boolean showMenuHint(Component shortcut) {
        Minecraft client=Minecraft.getInstance();
        ToastManager manager=getToastManager(client);if(manager==null)return false;
        try {
            menuHint=new SystemToast(new SystemToast.SystemToastId(5000L),Component.literal("Ji AFK Cinematic").withStyle(ChatFormatting.GOLD),
                    Component.translatable("overlay.ji_afkcinematic.menu_hint",shortcut)) {
                @Override public void render(net.minecraft.client.gui.GuiGraphics g,net.minecraft.client.gui.Font font,long time) {
                    super.render(g,font,time);
                    g.fill(4,10,16,21,0xFFBD97DB);g.fill(6,8,11,10,0xFFBD97DB);
                    g.fill(5,11,15,20,0xFF30243D);g.fill(8,12,13,18,0xFFFFD75E);
                    g.fill(9,13,12,17,0xFF30243D);g.fill(13,11,14,12,0xFFFFFFFF);

                }
            };
            manager.addToast(menuHint);return true;
        } catch(RuntimeException|LinkageError unavailable) { JiAFKCinematic.LOGGER.warn("Could not show first-world menu hint",unavailable);return false; }
    }

    private static void showMessage(Component title) {
        Minecraft client = Minecraft.getInstance();
        if (client == null) return;
        ToastManager manager = getToastManager(client);
        if (manager == null) return;



        // Guard the SystemToast constructor: if the SystemToastId/SystemToast signature
        // changes in a future MC version we degrade gracefully (no toast) instead of
        // throwing on the client thread.
        try {
            manager.addToast(new SystemToast(
                new SystemToast.SystemToastId(DISPLAY_MS),
                title,
                null
            ));
        } catch (Throwable t) {
            JiAFKCinematic.LOGGER.warn("Could not show toggle toast", t);
        }
    }

    private static ToastManager getToastManager(Minecraft client) {
        return client.getToastManager();
    }
}
