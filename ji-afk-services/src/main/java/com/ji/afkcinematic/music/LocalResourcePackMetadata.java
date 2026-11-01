package com.ji.afkcinematic.music;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Metadata for a generated pack, restricted to the current client's resource format. */
public final class LocalResourcePackMetadata {
    private LocalResourcePackMetadata() { }

    public static Map<String, Object> create(int major, int minor) {
        if (major < 1 || minor < 0 || major < 65 && minor != 0) {
            throw new IllegalArgumentException("Invalid client resource pack version");
        }
        Map<String, Object> pack = new LinkedHashMap<>();
        pack.put("description", "Ji AFK Cinematic local music");
        if (major < 65) {
            pack.put("pack_format", major);
        } else {
            List<Integer> exact = List.of(major, minor);
            pack.put("min_format", exact);
            pack.put("max_format", exact);
        }
        return Map.of("pack", Collections.unmodifiableMap(pack));
    }
}
