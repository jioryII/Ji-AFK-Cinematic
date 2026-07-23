package com.ji.afkcinematic.render;

import com.ji.afkcinematic.cinematic.CinematicManager;
import com.ji.afkcinematic.cinematic.CinematicState;
import com.ji.afkcinematic.config.ConfigManager;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.util.math.Vec3d;

public class PlayerForceRenderer {
    public static boolean renderedThisFrame = false;

    public static void init() {
        WorldRenderEvents.START.register(context -> {
            renderedThisFrame = false;
        });

        WorldRenderEvents.AFTER_ENTITIES.register(context -> {
            if (CinematicManager.getState() == CinematicState.CINEMATIC_ACTIVE) {
                if (renderedThisFrame) return; // Prevent Z-fighting if natively rendered

                MinecraftClient client = MinecraftClient.getInstance();
                AbstractClientPlayerEntity player = client.player;
                if (player != null) {
                    Vec3d cameraPos = context.camera().getPos();
                    float tickDelta = context.tickCounter().getTickProgress(true);
                    Vec3d lerpedPos = player.getLerpedPos(tickDelta);
                    double x = lerpedPos.x - cameraPos.x;
                    double y = lerpedPos.y - cameraPos.y;
                    double z = lerpedPos.z - cameraPos.z;
                    int light = client.getEntityRenderDispatcher().getLight(player, tickDelta);
                    
                    VertexConsumerProvider consumers = context.consumers();
                    if (consumers != null) {
                        // Temporarily disable nametag rendering if configured
                        boolean originalNameTag = !client.options.hudHidden;
                        if (!false) {
                            client.options.hudHidden = true; // Hack to hide nametag temporarily in old versions
                        }
                        
                        client.getEntityRenderDispatcher().render(
                            player,
                            x, y, z,
                            player.getYaw(tickDelta),
                            context.matrixStack(),
                            consumers,
                            light
                        );
                        
                        if (!false) {
                            client.options.hudHidden = originalNameTag;
                        }
                    }
                }
            }
        });
    }
}
