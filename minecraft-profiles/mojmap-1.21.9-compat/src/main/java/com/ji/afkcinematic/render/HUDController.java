package com.ji.afkcinematic.render;

public class HUDController {
    private static boolean hidden = false;
    private static net.minecraft.client.Options leasedOptions;
    private static boolean originalHideGui;

    /** Expose vanilla's global HUD flag to every render callback, without saving it. */
    public static void beginFrame() {
        endFrame(); // Also releases a previous render that exited with an exception.
        if (!isSuppressed()) return;
        leasedOptions = net.minecraft.client.Minecraft.getInstance().options;
        originalHideGui = leasedOptions.hideGui;
        leasedOptions.hideGui = true;
    }

    public static void endFrame() {
        if (leasedOptions == null) return;
        leasedOptions.hideGui = originalHideGui;
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
