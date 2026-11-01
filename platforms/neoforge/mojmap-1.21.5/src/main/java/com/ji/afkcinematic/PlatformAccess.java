package com.ji.afkcinematic;

import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.fml.loading.FMLPaths;
import java.nio.file.Path;

public final class PlatformAccess {
    private PlatformAccess() {}
    public static Path configDir() { return FMLPaths.CONFIGDIR.get(); }
    public static Path gameDir() { return FMLPaths.GAMEDIR.get(); }
    // Mixin selection happens before Minecraft sets SharedConstants' game version.
    public static String minecraftVersion() { return FMLLoader.versionInfo().mcVersion(); }
    public static String modVersion() {
        return ModList.get().getModContainerById("ji_afk_cinematic")
                .map(container -> container.getModInfo().getVersion().toString()).orElse("unknown");
    }
}
