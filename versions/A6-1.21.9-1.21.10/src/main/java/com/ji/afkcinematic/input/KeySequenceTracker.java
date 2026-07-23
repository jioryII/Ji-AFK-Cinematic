package com.ji.afkcinematic.input;

import org.lwjgl.glfw.GLFW;

/**
 * Stateless-per-sequence tracker para secuencias de 2 teclas (timeout 1s).
 *
 * Disenado para evitar la interferencia cruzada entre secuencias que comparten
 * una tecla (por ejemplo, cuando dos secuencias usan "H" como segunda tecla).
 * Cada llamada mantiene su propio estado first-key, asi pulsar F7 (menu) nunca
 * puede entregarle un match a la secuencia toggle aunque ambas terminen en H.
 *
 * Tambien acepta L-Ctrl y R-Ctrl (idem Shift/Alt) como primera tecla
 * indistintamente, sin que el usuario tenga que rebindar.
 */
public final class KeySequenceTracker {
    private KeySequenceTracker() {}

    public static final long SEQUENCE_TIMEOUT_MS = 1500L;

    // === Estado runtime por secuencia ===
    private static int menuFirstKey = -1;
    private static long menuFirstKeyTime = 0L;
    private static int toggleFirstKey = -1;
    private static long toggleFirstKeyTime = 0L;

    // === Estado rebind (single, solo se rebinda una secuencia a la vez) ===
    private static int rebindFirstKey = -1;
    private static long rebindFirstTimeMs = 0L;

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

    private static boolean matchesAny(int keyCode, int[] candidates) {
        for (int k : candidates) if (k == keyCode) return true;
        return false;
    }

    /**
     * Secuencia del menu (default: F7 + H).
     */
    public static boolean checkMenu(int keyCode, int[] acceptedFirstKeys, int secondKey) {
        return check(keyCode, acceptedFirstKeys, secondKey, true);
    }

    /**
     * Secuencia toggle rapido (default: Ctrl + H).
     */
    public static boolean checkToggle(int keyCode, int[] acceptedFirstKeys, int secondKey) {
        return check(keyCode, acceptedFirstKeys, secondKey, false);
    }

    private static boolean check(int keyCode, int[] acceptedFirstKeys, int secondKey, boolean isMenu) {
        long now = System.currentTimeMillis();

        int firstKey = isMenu ? menuFirstKey : toggleFirstKey;
        long firstTime = isMenu ? menuFirstKeyTime : toggleFirstKeyTime;

        // Single-key sequence (acceptedFirstKeys contains secondKey only)
        if (acceptedFirstKeys.length == 1 && acceptedFirstKeys[0] == secondKey) {
            if (keyCode == secondKey) {
                resetSequence(isMenu);
                return true;
            }
            return false;
        }

        if (firstKey == -1) {
            if (matchesAny(keyCode, acceptedFirstKeys)) {
                setFirst(isMenu, keyCode, now);
            }
            return false;
        }

        if (now - firstTime > SEQUENCE_TIMEOUT_MS) {
            resetSequence(isMenu);
            if (matchesAny(keyCode, acceptedFirstKeys)) {
                setFirst(isMenu, keyCode, now);
            }
            return false;
        }

        if (keyCode == secondKey) {
            // Verify our recorded firstKey belongs to THIS sequence (defense in depth)
            if (matchesAny(firstKey, acceptedFirstKeys)) {
                resetSequence(isMenu);
                return true;
            }
            resetSequence(isMenu);
            return false;
        }

        // Other key pressed mid-sequence
        resetSequence(isMenu);
        if (matchesAny(keyCode, acceptedFirstKeys)) {
            setFirst(isMenu, keyCode, now);
        }
        return false;
    }

    private static void setFirst(boolean isMenu, int keyCode, long now) {
        if (isMenu) { menuFirstKey = keyCode; menuFirstKeyTime = now; }
        else        { toggleFirstKey = keyCode; toggleFirstKeyTime = now; }
    }

    public static void resetSequence(boolean isMenu) {
        if (isMenu) { menuFirstKey = -1; menuFirstKeyTime = 0L; }
        else        { toggleFirstKey = -1; toggleFirstKeyTime = 0L; }
    }

    public static void resetAll() {
        menuFirstKey = -1; menuFirstKeyTime = 0L;
        toggleFirstKey = -1; toggleFirstKeyTime = 0L;
    }

    // === Rebind UI (separado del runtime) ===

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
     *  -1 = timeout/cancelado
     */
    public static int processRebindKey(int keyCode, int[] outKeys) {
        long now = System.currentTimeMillis();

        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            resetRebind();
            return -1;
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

        outKeys[0] = rebindFirstKey;
        outKeys[1] = keyCode;
        resetRebind();
        return 2;
    }

    public static void resetRebind() {
        rebindFirstKey = -1;
        rebindFirstTimeMs = 0L;
    }
}
