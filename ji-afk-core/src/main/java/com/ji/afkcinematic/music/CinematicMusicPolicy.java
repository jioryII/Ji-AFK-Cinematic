package com.ji.afkcinematic.music;

import java.util.Locale;
import java.util.Set;

/** Permanent exclusions apply both to declared events and their resolved audio files. */
public final class CinematicMusicPolicy {
    private static final Set<String> DISCS = Set.of("5", "11", "13");
    private CinematicMusicPolicy() {}

    public static boolean isForbidden(String identifier) {
        if (identifier == null) return false;
        String id = identifier.trim().toLowerCase(Locale.ROOT).replace('\\', '/');
        int namespace = id.indexOf(':');
        String path = namespace < 0 ? id : id.substring(namespace + 1);
        if (path.endsWith(".ogg")) path = path.substring(0, path.length() - 4);
        for (String disc : DISCS) {
            if (path.equals("music_disc." + disc) || path.equals("music_disc_" + disc)
                    || path.equals("records/" + disc) || path.endsWith("/records/" + disc)
                    || path.equals("record." + disc)) return true;
        }
        return false;
    }
}
