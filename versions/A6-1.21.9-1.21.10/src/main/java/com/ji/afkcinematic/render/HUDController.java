package com.ji.afkcinematic.render;

public class HUDController {
    private static boolean hidden = false;

    public static void setHidden(boolean hide) {
        hidden = hide;
    }

    public static boolean isHidden() {
        return hidden;
    }
}
