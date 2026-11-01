package com.ji.afkcinematic.diagnostic;

import com.ji.afkcinematic.afk.AFKDetector;
import com.ji.afkcinematic.config.ConfigManager;
import java.util.Locale;
import java.util.Map;

/** Called on the client thread by an explicit preview action. Metadata comes from the active loader. */
public final class RuntimeDiagnostics {
    private RuntimeDiagnostics() {}
    public static DiagnosticReport.Build build() { return DiagnosticPlatform.build(); }

    public static DiagnosticReport.Snapshot capture() {
        var cfg = ConfigManager.getConfig();
        var session = AFKDetector.session();
        String os = System.getProperty("os.name", "unknown").toLowerCase(Locale.ROOT);
        var family = os.contains("win") ? DiagnosticReport.Os.windows : os.contains("mac") ? DiagnosticReport.Os.macos
                : os.contains("linux") ? DiagnosticReport.Os.linux : DiagnosticReport.Os.unknown;
        var metadata = new DiagnosticReport.Runtime(property("java.version"), property("java.vendor"), family,
                property("os.version"), property("os.arch"),
                build().mc().equals("26.3") ? DiagnosticReport.Window.sdl : DiagnosticReport.Window.glfw,
                DiagnosticReport.Graphics.unknown);
        var config = new DiagnosticReport.Configuration(cfg.modEnabled, cfg.afkThresholdSeconds,
                cfg.shotDurationSeconds, cfg.maxCycles, cfg.persistentMode,
                DiagnosticReport.Music.valueOf(cfg.musicMode.name()), !cfg.customMusicDirectory.isBlank());
        return new DiagnosticReport.Snapshot(build(), metadata, DiagnosticPlatform.mods(), config,
                session.snapshot(), session.snapshot().epoch() > 1, HookCapabilities.snapshot(), Map.of(DiagnosticReport.Compatibility.spatialGui, RuntimeResilience.spatialPresent(), DiagnosticReport.Compatibility.smoothF5, com.ji.afkcinematic.compat.SmoothF5Compatibility.present()), session.traceSnapshot(), RuntimeResilience.incidents());
    }
    private static String property(String name) {
        String value = System.getProperty(name, "unknown");
        if (value.isBlank() || value.length() > 128 || value.chars().anyMatch(c -> c < 32)) return "unknown";
        return value;
    }
}
