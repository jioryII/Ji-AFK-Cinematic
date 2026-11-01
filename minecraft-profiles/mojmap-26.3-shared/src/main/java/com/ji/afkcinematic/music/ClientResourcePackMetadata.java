package com.ji.afkcinematic.music;

import java.util.Map;

/** Native resource version adapter; never advertises a future format. */
final class ClientResourcePackMetadata {
    private ClientResourcePackMetadata() { }
    static Map<String, Object> create() {
        var format = net.minecraft.SharedConstants.getCurrentVersion().packVersion(net.minecraft.server.packs.PackType.CLIENT_RESOURCES);
        return LocalResourcePackMetadata.create(format.major(), format.minor());
    }
}
