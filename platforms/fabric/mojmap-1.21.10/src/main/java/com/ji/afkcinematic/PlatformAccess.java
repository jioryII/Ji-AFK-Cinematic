package com.ji.afkcinematic;

import net.fabricmc.loader.api.FabricLoader;
import java.nio.file.Path;

/** Loader-specific paths and version metadata used by the shared client code. */
public final class PlatformAccess {
    private PlatformAccess() {}

    public static Path configDir() { return FabricLoader.getInstance().getConfigDir(); }
    public static Path gameDir() { return FabricLoader.getInstance().getGameDir(); }
    public static String minecraftVersion() {
        return FabricLoader.getInstance().getModContainer("minecraft")
                .map(container -> container.getMetadata().getVersion().getFriendlyString()).orElse("");
    }
    public static String modVersion() {
        return FabricLoader.getInstance().getModContainer(JiAFKCinematic.MOD_ID)
                .map(container -> container.getMetadata().getVersion().getFriendlyString()).orElse("unknown");
    }
}
