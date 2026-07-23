package com.ji.afkcinematic.mixin;

import com.ji.afkcinematic.cinematic.CinematicManager;
import com.ji.afkcinematic.cinematic.CinematicState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import com.ji.afkcinematic.render.PlayerForceRenderer;

@Mixin(EntityRenderDispatcher.class)
public class EntityRenderDispatcherMixin {

    @Inject(method = "render", at = @At("HEAD"), require = 0)
    private <E extends Entity> void trackPlayerRender(E entity, double x, double y, double z, float yaw, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        if (CinematicManager.getState() == CinematicState.CINEMATIC_ACTIVE) {
            MinecraftClient client = MinecraftClient.getInstance();
            if (entity == client.player) {
                PlayerForceRenderer.renderedThisFrame = true;
            }
        }
    }
}
