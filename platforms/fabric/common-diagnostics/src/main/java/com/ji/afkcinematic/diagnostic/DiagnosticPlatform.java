package com.ji.afkcinematic.diagnostic;

import com.ji.afkcinematic.JiAFKCinematic;
import net.fabricmc.loader.api.FabricLoader;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/** Loader metadata only; no world/player/server/log data is requested. */
public final class DiagnosticPlatform {
    private DiagnosticPlatform() {}
    private static String version(String id) {
        return FabricLoader.getInstance().getModContainer(id)
                .map(mod -> mod.getMetadata().getVersion().getFriendlyString()).orElse("unknown");
    }
    public static DiagnosticReport.Build build() {
        String api = version("fabric-api");
        return new DiagnosticReport.Build(version(JiAFKCinematic.MOD_ID), version("minecraft"),
                DiagnosticReport.Loader.fabric, version("fabricloader"),
                api.equals("unknown") ? Map.of() : Map.of(DiagnosticReport.Loader.fabric, api));
    }
    public static List<DiagnosticReport.Mod> mods() {
        return FabricLoader.getInstance().getAllMods().stream()
                .map(mod -> new DiagnosticReport.Mod(mod.getMetadata().getId(), mod.getMetadata().getVersion().getFriendlyString())).toList();
    }
    public static Path configDir() { return FabricLoader.getInstance().getConfigDir(); }
}
