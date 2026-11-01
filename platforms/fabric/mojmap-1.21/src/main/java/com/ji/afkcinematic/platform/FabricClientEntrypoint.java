package com.ji.afkcinematic.platform;

import com.ji.afkcinematic.JiAFKCinematic;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;

public final class FabricClientEntrypoint implements ClientModInitializer {
    @Override
    @SuppressWarnings("deprecation")
    public void onInitializeClient() {
        ClientTickEvents.START_CLIENT_TICK.register(ClientEvents.START_CLIENT_TICK::fire);
        ClientTickEvents.END_CLIENT_TICK.register(ClientEvents.END_CLIENT_TICK::fire);
        ClientLifecycleEvents.CLIENT_STARTED.register(ClientEvents.CLIENT_STARTED::fire);
        ClientLifecycleEvents.CLIENT_STOPPING.register(ClientEvents.CLIENT_STOPPING::fire);
        HudRenderCallback.EVENT.register(
                (graphics, delta) -> ClientEvents.HUD.fire(new ClientEvents.HudFrame(graphics, delta)));
        ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(
                new SimpleSynchronousResourceReloadListener() {
                    @Override public ResourceLocation getFabricId() {
                        return ResourceLocation.fromNamespaceAndPath(JiAFKCinematic.MOD_ID, "music_catalog");
                    }
                    @Override public void onResourceManagerReload(ResourceManager manager) {
                        ClientEvents.RESOURCE_RELOAD.fire(manager);
                    }
                });
        JiAFKCinematic.initializeClient();
    }
}
