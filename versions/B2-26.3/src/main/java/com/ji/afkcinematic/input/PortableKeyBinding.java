package com.ji.afkcinematic.input;

import com.mojang.blaze3d.platform.InputConstants;

/** Converts native input codes to the stable vanilla key names stored in config. */
public final class PortableKeyBinding {
    public static final String DISABLED = "disabled";

    private PortableKeyBinding() {}

    public static String nameOf(int nativeCode) {
        if (nativeCode == -1) return DISABLED;
        try {
            InputConstants.Key key = InputConstants.Type.KEYBOARD.getOrCreate(nativeCode);
            return key == InputConstants.UNKNOWN ? null : key.getName();
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    public static int codeOf(String portableName, int fallback) {
        if (DISABLED.equals(portableName)) return -1;
        if (portableName == null || portableName.isBlank()) return fallback;
        try {
            InputConstants.Key key = InputConstants.getKey(portableName);
            return key == InputConstants.UNKNOWN || key.getType() != InputConstants.Type.KEYBOARD
                    ? fallback : key.getValue();
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }
}
