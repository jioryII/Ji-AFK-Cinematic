package com.ji.afkcinematic.platform;

import com.ji.afkcinematic.JiAFKCinematic;
import com.ji.afkcinematic.config.ConfigScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.event.GameShuttingDownEvent;

@Mod(value = "ji_afk_cinematic", dist = Dist.CLIENT)
public final class NeoForgeClientEntrypoint {
    private boolean clientStarted;
    public NeoForgeClientEntrypoint(IEventBus modBus, ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class,
                (modContainer, parent) -> new ConfigScreen(parent));
        modBus.addListener(this::registerReloadListener);
        modBus.addListener(this::clientSetup);
        // One native dispatch per phase preserves the same callback order as Fabric.
        NeoForge.EVENT_BUS.addListener(this::tickStart);
        NeoForge.EVENT_BUS.addListener(this::tickEnd);
        NeoForge.EVENT_BUS.addListener(this::clientStopping);
        NeoForge.EVENT_BUS.addListener(this::renderHud);
    }

    private void clientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(JiAFKCinematic::initializeClient);
    }
    private void registerReloadListener(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener((ResourceManagerReloadListener) ClientEvents.RESOURCE_RELOAD::fire);
    }
    private void tickStart(ClientTickEvent.Pre event) {
        Minecraft client = Minecraft.getInstance();
        // 21.3 has no ClientStartedEvent; emit once after client construction.
        if (!clientStarted) {
            clientStarted = true;
            ClientEvents.CLIENT_STARTED.fire(client);
        }
        ClientEvents.START_CLIENT_TICK.fire(client);
    }
    private void tickEnd(ClientTickEvent.Post event) {
        ClientEvents.END_CLIENT_TICK.fire(Minecraft.getInstance());
    }
    private void clientStopping(GameShuttingDownEvent event) {
        ClientEvents.CLIENT_STOPPING.fire(Minecraft.getInstance());
    }
    private void renderHud(RenderGuiEvent.Post event) {
        ClientEvents.HUD.fire(new ClientEvents.HudFrame(event.getGuiGraphics(), event.getPartialTick()));
    }
}
