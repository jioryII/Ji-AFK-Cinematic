package com.ji.afkcinematic.platform;

import net.minecraft.client.Minecraft;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.server.packs.resources.ResourceManager;

/** Minecraft-typed ports. Each loader installs its own native event bridge. */
public final class ClientEvents {
    public static final OrderedClientEvent<Minecraft> START_CLIENT_TICK = new OrderedClientEvent<>();
    public static final OrderedClientEvent<Minecraft> END_CLIENT_TICK = new OrderedClientEvent<>();
    public static final OrderedClientEvent<Minecraft> CLIENT_STARTED = new OrderedClientEvent<>();
    public static final OrderedClientEvent<Minecraft> CLIENT_STOPPING = new OrderedClientEvent<>();
    public static final OrderedClientEvent<HudFrame> HUD = new OrderedClientEvent<>();
    public static final OrderedClientEvent<ResourceManager> RESOURCE_RELOAD = new OrderedClientEvent<>();

    public record HudFrame(GuiGraphics graphics, DeltaTracker delta) {}
    private ClientEvents() {}
}
