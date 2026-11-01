package com.ji.afkcinematic.render;

public class HUDController {
    private static boolean hidden = false;
    private static final boolean LEGACY = com.ji.afkcinematic.PlatformAccess.minecraftVersion().startsWith("26.1");
    private static com.ji.afkcinematic.mixin.LegacyOptionsHudAccess leasedOptions;
    private static boolean originalHideGui;

    public static void beginFrame() {
        endFrame();
        if (!LEGACY || !isSuppressed()) return;
        leasedOptions = (com.ji.afkcinematic.mixin.LegacyOptionsHudAccess) (Object)
                net.minecraft.client.Minecraft.getInstance().options;
        originalHideGui = leasedOptions.jiAfk$hidden();
        leasedOptions.jiAfk$hidden(true);
    }

    public static void endFrame() {
        if (leasedOptions == null) return;
        leasedOptions.jiAfk$hidden(originalHideGui);
        leasedOptions = null;
    }

    public static void setHidden(boolean hide) {
        hidden = hide;
        if (!hide) endFrame();
    }

    public static boolean isSuppressed() {
        return hidden || LetterboxRenderer.isSleepActive();
    }

    public static boolean isHidden() {
        return hidden;
    }
}
