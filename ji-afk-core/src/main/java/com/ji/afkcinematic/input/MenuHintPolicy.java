package com.ji.afkcinematic.input;
/** The first-world hint must never advertise a disabled or Escape shortcut. */
public final class MenuHintPolicy {
    private MenuHintPolicy() {}
    public static boolean eligible(boolean enabled,String first,String second) {
        return enabled && key(first) && key(second);
    }
    private static boolean key(String key) {
        return key!=null && key.startsWith("key.keyboard.") && !key.equals("key.keyboard.escape") && !key.equals("key.keyboard.unknown");
    }
}
