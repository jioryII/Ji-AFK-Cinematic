package com.ji.afkcinematic.diagnostic;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Tracks which of this mod's mixins actually applied at runtime. Populated by
 * the mixin config plugin's post-apply callback (which only fires for mixins
 * that successfully bound to their target) and read by the main entrypoint to log a
 * WARNING for any critical mixin whose {@code require = 0} injector silently
 * no-op'd against a future Minecraft version.
 *
 * Thread-safety: mixin application happens single-threaded on the class-load
 * phase, before any client tick; reads from the entrypoint happen after. A plain
 * set is sufficient.
 */
public final class MixinState {
    private static final Set<String> APPLIED = new LinkedHashSet<>();

    private MixinState() {}

    public static void markApplied(String mixinClassName) {
        if (mixinClassName == null) return;
        // store the simple name (strip package prefix) for readable logs
        String simple = mixinClassName;
        int dot = mixinClassName.lastIndexOf('.');
        if (dot >= 0) simple = mixinClassName.substring(dot + 1);
        APPLIED.add(simple);
    }

    /** @return an unmodifiable view of the simple names of mixins that applied. */
    public static Set<String> getApplied() {
        return Collections.unmodifiableSet(APPLIED);
    }

    public static boolean didApply(String simpleName) {
        return APPLIED.contains(simpleName);
    }
}
