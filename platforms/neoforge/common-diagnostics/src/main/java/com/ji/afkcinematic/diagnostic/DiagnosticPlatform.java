package com.ji.afkcinematic.diagnostic;

import com.ji.afkcinematic.PlatformAccess;
import net.neoforged.fml.ModList;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public final class DiagnosticPlatform {
    private DiagnosticPlatform() {}
    public static DiagnosticReport.Build build() {
        String version = ModList.get().getModContainerById("neoforge")
                .map(mod -> mod.getModInfo().getVersion().toString()).orElse("unknown");
        return new DiagnosticReport.Build(PlatformAccess.modVersion(), PlatformAccess.minecraftVersion(),
                DiagnosticReport.Loader.neoforge, version, Map.of(DiagnosticReport.Loader.neoforge, version));
    }
    public static List<DiagnosticReport.Mod> mods() {
        return ModList.get().getMods().stream().map(mod -> new DiagnosticReport.Mod(mod.getModId(), mod.getVersion().toString())).toList();
    }
    public static Path configDir() { return PlatformAccess.configDir(); }
}
