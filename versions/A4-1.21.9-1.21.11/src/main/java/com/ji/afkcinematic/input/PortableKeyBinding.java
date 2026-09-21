package com.ji.afkcinematic.input;

import net.minecraft.client.util.InputUtil;

/** Converts native input codes to the stable vanilla key names stored in config. */
public final class PortableKeyBinding {
    public static final String DISABLED = "disabled";

    private PortableKeyBinding() {}

    public static String nameOf(int nativeCode) {
        if (nativeCode == -1) return DISABLED;
        try {
            InputUtil.Key key = InputUtil.Type.KEYSYM.createFromCode(nativeCode);
            return key == InputUtil.UNKNOWN_KEY ? null : key.getTranslationKey();
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    public static int codeOf(String portableName, int fallback) {
        if (DISABLED.equals(portableName)) return -1;
        if (portableName == null || portableName.isBlank()) return fallback;
        try {
            InputUtil.Key key = InputUtil.fromTranslationKey(portableName);
            return key == InputUtil.UNKNOWN_KEY || key.getCategory() != InputUtil.Type.KEYSYM
                    ? fallback : key.getCode();
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }
}
