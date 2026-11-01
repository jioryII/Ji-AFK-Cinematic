package com.ji.afkcinematic.input;

/** Portable identities, independent of the GLFW/SDL codes used by the active client. */
public final class ShortcutDefaults {
    private ShortcutDefaults() { }

    public static boolean isLegacyToggle(String first, String second) {
        return ("key.keyboard.left.control".equals(first) || "key.keyboard.right.control".equals(first))
                && "key.keyboard.h".equals(second)
                || "key.keyboard.f7".equals(first) && "key.keyboard.j".equals(second);
    }
}
