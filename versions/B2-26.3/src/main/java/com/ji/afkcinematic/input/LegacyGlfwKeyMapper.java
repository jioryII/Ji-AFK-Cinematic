package com.ji.afkcinematic.input;

import com.mojang.blaze3d.platform.InputConstants;

/** Converts saved pre-SDL shortcut codes to the current vanilla input constants. */
public final class LegacyGlfwKeyMapper {
    private static final int[] LETTERS = {
            InputConstants.KEY_A, InputConstants.KEY_B, InputConstants.KEY_C, InputConstants.KEY_D,
            InputConstants.KEY_E, InputConstants.KEY_F, InputConstants.KEY_G, InputConstants.KEY_H,
            InputConstants.KEY_I, InputConstants.KEY_J, InputConstants.KEY_K, InputConstants.KEY_L,
            InputConstants.KEY_M, InputConstants.KEY_N, InputConstants.KEY_O, InputConstants.KEY_P,
            InputConstants.KEY_Q, InputConstants.KEY_R, InputConstants.KEY_S, InputConstants.KEY_T,
            InputConstants.KEY_U, InputConstants.KEY_V, InputConstants.KEY_W, InputConstants.KEY_X,
            InputConstants.KEY_Y, InputConstants.KEY_Z
    };
    private static final int[] DIGITS = {
            InputConstants.KEY_0, InputConstants.KEY_1, InputConstants.KEY_2, InputConstants.KEY_3,
            InputConstants.KEY_4, InputConstants.KEY_5, InputConstants.KEY_6, InputConstants.KEY_7,
            InputConstants.KEY_8, InputConstants.KEY_9
    };
    private static final int[] FUNCTION_KEYS = {
            InputConstants.KEY_F1, InputConstants.KEY_F2, InputConstants.KEY_F3,
            InputConstants.KEY_F4, InputConstants.KEY_F5, InputConstants.KEY_F6,
            InputConstants.KEY_F7, InputConstants.KEY_F8, InputConstants.KEY_F9,
            InputConstants.KEY_F10, InputConstants.KEY_F11, InputConstants.KEY_F12,
            InputConstants.KEY_F13, InputConstants.KEY_F14, InputConstants.KEY_F15,
            InputConstants.KEY_F16, InputConstants.KEY_F17, InputConstants.KEY_F18,
            InputConstants.KEY_F19, InputConstants.KEY_F20, InputConstants.KEY_F21,
            InputConstants.KEY_F22, InputConstants.KEY_F23, InputConstants.KEY_F24
    };

    private LegacyGlfwKeyMapper() {}

    /** -1 remains disabled; -2 asks ModConfig to restore an unsupported custom binding. */
    public static int migrate(int legacyCode) {
        if (legacyCode == -1) return -1;
        if (legacyCode >= 65 && legacyCode <= 90) return LETTERS[legacyCode - 65];
        if (legacyCode >= 48 && legacyCode <= 57) return DIGITS[legacyCode - 48];
        if (legacyCode >= 290 && legacyCode <= 313) return FUNCTION_KEYS[legacyCode - 290];
        return switch (legacyCode) {
            case 32 -> InputConstants.KEY_SPACE;
            case 39 -> InputConstants.KEY_APOSTROPHE;
            case 44 -> InputConstants.KEY_COMMA;
            case 45 -> InputConstants.KEY_MINUS;
            case 46 -> InputConstants.KEY_PERIOD;
            case 47 -> InputConstants.KEY_SLASH;
            case 59 -> InputConstants.KEY_SEMICOLON;
            case 61 -> InputConstants.KEY_EQUALS;
            case 91 -> InputConstants.KEY_LBRACKET;
            case 92 -> InputConstants.KEY_BACKSLASH;
            case 93 -> InputConstants.KEY_RBRACKET;
            case 96 -> InputConstants.KEY_GRAVE;
            case 256 -> InputConstants.KEY_ESCAPE;
            case 257 -> InputConstants.KEY_RETURN;
            case 258 -> InputConstants.KEY_TAB;
            case 259 -> InputConstants.KEY_BACKSPACE;
            case 260 -> InputConstants.KEY_INSERT;
            case 261 -> InputConstants.KEY_DELETE;
            case 262 -> InputConstants.KEY_RIGHT;
            case 263 -> InputConstants.KEY_LEFT;
            case 264 -> InputConstants.KEY_DOWN;
            case 265 -> InputConstants.KEY_UP;
            case 266 -> InputConstants.KEY_PAGEUP;
            case 267 -> InputConstants.KEY_PAGEDOWN;
            case 268 -> InputConstants.KEY_HOME;
            case 269 -> InputConstants.KEY_END;
            case 280 -> InputConstants.KEY_CAPSLOCK;
            case 281 -> InputConstants.KEY_SCROLLLOCK;
            case 282 -> InputConstants.KEY_NUMLOCK;
            case 283 -> InputConstants.KEY_PRINTSCREEN;
            case 284 -> InputConstants.KEY_PAUSE;
            case 320 -> InputConstants.KEY_NUMPAD0;
            case 321 -> InputConstants.KEY_NUMPAD1;
            case 322 -> InputConstants.KEY_NUMPAD2;
            case 323 -> InputConstants.KEY_NUMPAD3;
            case 324 -> InputConstants.KEY_NUMPAD4;
            case 325 -> InputConstants.KEY_NUMPAD5;
            case 326 -> InputConstants.KEY_NUMPAD6;
            case 327 -> InputConstants.KEY_NUMPAD7;
            case 328 -> InputConstants.KEY_NUMPAD8;
            case 329 -> InputConstants.KEY_NUMPAD9;
            case 330 -> InputConstants.KEY_NUMPADCOMMA;
            case 332 -> InputConstants.KEY_MULTIPLY;
            case 334 -> InputConstants.KEY_ADD;
            case 335 -> InputConstants.KEY_NUMPADENTER;
            case 336 -> InputConstants.KEY_NUMPADEQUALS;
            case 340 -> InputConstants.KEY_LSHIFT;
            case 341 -> InputConstants.KEY_LCONTROL;
            case 342 -> InputConstants.KEY_LALT;
            case 343 -> InputConstants.KEY_LGUI;
            case 344 -> InputConstants.KEY_RSHIFT;
            case 345 -> InputConstants.KEY_RCONTROL;
            case 346 -> InputConstants.KEY_RALT;
            case 347 -> InputConstants.KEY_RGUI;
            default -> -2;
        };
    }

    public static boolean isSupported(int legacyCode) {
        return legacyCode == -1 || migrate(legacyCode) != -2;
    }
}
