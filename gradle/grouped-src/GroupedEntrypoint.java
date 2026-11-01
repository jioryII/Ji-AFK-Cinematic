package com.ji.afkcinematic.group;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

/** Only this class is annotated: inactive variants never register callbacks. */
@Mod(value = "ji_afk_cinematic", dist = Dist.CLIENT)
public final class GroupedEntrypoint {
    public GroupedEntrypoint(IEventBus bus, ModContainer container) {
        String version = GroupSelection.version();
        String entrypoint = GroupSelection.properties("META-INF/ji-afk-group.properties").getProperty(version);
        if (entrypoint == null) throw new IllegalStateException("Unsupported Minecraft: " + version);
        try {
            Class.forName(entrypoint).getConstructor(IEventBus.class, ModContainer.class).newInstance(bus, container);
        } catch (ReflectiveOperationException error) {
            throw GroupSelection.failure("Cannot initialize Ji AFK Cinematic for " + version, error);
        }
    }
}
