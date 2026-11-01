package com.ji.afkcinematic.input;

import org.lwjgl.glfw.GLFW;

/** Native key aliases and rebind capture. Runtime resolution belongs to ShortcutController. */
public final class KeySequenceTracker {
    private KeySequenceTracker() {}
    public static final long SEQUENCE_TIMEOUT_MS = 1500L;
    public static final int UNSUPPORTED_KEY_REJECTED = 3;
    private static int rebindFirstKey = -1;
    private static long rebindFirstTimeMs;

    public static boolean isBindableKeyCode(int keyCode) {
        return keyCode >= GLFW.GLFW_KEY_SPACE && keyCode <= GLFW.GLFW_KEY_LAST;
    }

    /**
     * Dado un key configurado por el usuario, devuelve el conjunto de teclas
     * aceptadas como primera tecla. Si es un modificador (Ctrl/Shift/Alt),
     * acepta ambos lados del teclado.
     */
    public static int[] acceptedFirstKeys(int configuredKey) {
        if (configuredKey == GLFW.GLFW_KEY_LEFT_CONTROL || configuredKey == GLFW.GLFW_KEY_RIGHT_CONTROL) {
            return new int[]{ GLFW.GLFW_KEY_LEFT_CONTROL, GLFW.GLFW_KEY_RIGHT_CONTROL };
        }
        if (configuredKey == GLFW.GLFW_KEY_LEFT_SHIFT || configuredKey == GLFW.GLFW_KEY_RIGHT_SHIFT) {
            return new int[]{ GLFW.GLFW_KEY_LEFT_SHIFT, GLFW.GLFW_KEY_RIGHT_SHIFT };
        }
        if (configuredKey == GLFW.GLFW_KEY_LEFT_ALT || configuredKey == GLFW.GLFW_KEY_RIGHT_ALT) {
            return new int[]{ GLFW.GLFW_KEY_LEFT_ALT, GLFW.GLFW_KEY_RIGHT_ALT };
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

        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            resetRebind();
            return -1;
        }

        // GLFW entrega teclas multimedia no representables como KEY_UNKNOWN.
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
