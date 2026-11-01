package com.ji.afkcinematic.music;
import java.util.Map;
final class ClientResourcePackMetadata {
    private ClientResourcePackMetadata() { }
    static Map<String, Object> create() {
        int format = net.minecraft.SharedConstants.getCurrentVersion().packVersion(net.minecraft.server.packs.PackType.CLIENT_RESOURCES);
        return LocalResourcePackMetadata.create(format, 0);
    }
}
