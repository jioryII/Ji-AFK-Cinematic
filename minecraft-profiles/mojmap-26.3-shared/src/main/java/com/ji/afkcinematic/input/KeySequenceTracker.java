package com.ji.afkcinematic.input;

import com.mojang.blaze3d.platform.InputConstants;

/** Native key aliases and rebind capture. Runtime resolution belongs to ShortcutController. */
public final class KeySequenceTracker {
    private KeySequenceTracker() {}
    public static final long SEQUENCE_TIMEOUT_MS = 1500L;
    public static final int UNSUPPORTED_KEY_REJECTED = 3;
    private static int rebindFirstKey = -1;
    private static long rebindFirstTimeMs;

    public static boolean isBindableKeyCode(int keyCode) {
        return keyCode > 0 && keyCode <= 65535;
    }

    /**
     * Dado un key configurado por el usuario, devuelve el conjunto de teclas
     * aceptadas como primera tecla. Si es un modificador (Ctrl/Shift/Alt),
     * acepta ambos lados del teclado.
     */
    public static int[] acceptedFirstKeys(int configuredKey) {
        if (configuredKey == InputConstants.KEY_LCONTROL || configuredKey == InputConstants.KEY_RCONTROL) {
            return new int[]{ InputConstants.KEY_LCONTROL, InputConstants.KEY_RCONTROL };
        }
        if (configuredKey == InputConstants.KEY_LSHIFT || configuredKey == InputConstants.KEY_RSHIFT) {
            return new int[]{ InputConstants.KEY_LSHIFT, InputConstants.KEY_RSHIFT };
        }
        if (configuredKey == InputConstants.KEY_LALT || configuredKey == InputConstants.KEY_RALT) {
            return new int[]{ InputConstants.KEY_LALT, InputConstants.KEY_RALT };
        }
        return new int[]{ configuredKey };
    }

    public static void resetAll() { RuntimeShortcuts.cancelPending(); }

    public static void startRebind() {
        rebindFirstKey = -1;
        rebindFirstTimeMs = 0L;
    }

    public static boolean hasRebindFirst() { return rebindFirstKey != -1; }
    public static int getRebindFirst() { return rebindFirstKey; }

    public static long getRebindRemainingMs() {
        if (rebindFirstKey == -1) return 0L;
        return Math.max(0L, SEQUENCE_TIMEOUT_MS - (System.currentTimeMillis() - rebindFirstTimeMs));
    }

    /**
     * Procesa una tecla durante el rebind. Devuelve:
     *   0 = aun no completo (esperando segunda)
     *   1 = primera tecla capturada
     *   2 = segunda tecla capturada (par listo)
     *   3 = {@link #UNSUPPORTED_KEY_REJECTED} — tecla desconocida/no bindeable
     *  -1 = timeout/cancelado
     */
    public static int processRebindKey(int keyCode, int[] outKeys) {
        long now = System.currentTimeMillis();

        if (keyCode == InputConstants.KEY_ESCAPE) {
            resetRebind();
            return -1;
        }

        // Unsupported multimedia keys may arrive as the unknown scancode.
        // No consumimos el evento para que el usuario pueda elegir otra tecla.
        if (!isBindableKeyCode(keyCode)) {
            return UNSUPPORTED_KEY_REJECTED;
        }

        if (rebindFirstKey == -1) {
            rebindFirstKey = keyCode;
            rebindFirstTimeMs = now;
            outKeys[0] = keyCode;
            return 1;
        }

        if (now - rebindFirstTimeMs > SEQUENCE_TIMEOUT_MS) {
            resetRebind();
            return -1;
        }

        // Auto-repeat and a repeated physical key never form a two-key chord.
        if (keyCode == rebindFirstKey) return 0;

        outKeys[0] = rebindFirstKey;
        outKeys[1] = keyCode;
        resetRebind();
        return 2;
    }

    /** Release the first key to choose a single key, using the legacy pair encoding. */
    public static int processRebindRelease(int keyCode, int[] outKeys) {
        if (rebindFirstKey == -1 || keyCode != rebindFirstKey) return 0;
        if (getRebindRemainingMs() <= 0L) {
            resetRebind();
            return -1;
        }
        outKeys[0] = rebindFirstKey;
        outKeys[1] = rebindFirstKey;
        resetRebind();
        return 2;
    }

    public static void resetRebind() {
        rebindFirstKey = -1;
        rebindFirstTimeMs = 0L;
    }
}
