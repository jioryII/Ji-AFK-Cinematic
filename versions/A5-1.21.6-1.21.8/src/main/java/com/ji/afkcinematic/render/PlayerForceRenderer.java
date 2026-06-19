package com.ji.afkcinematic.render;

import com.ji.afkcinematic.cinematic.CinematicManager;
import com.ji.afkcinematic.cinematic.CinematicState;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.util.math.Vec3d;

import net.minecraft.util.math.MathHelper;

public class PlayerForceRenderer {
    public static void init() {
        // Disabled: Native third-person camera renders the player perfectly and handles chunk loading.
    }
}
