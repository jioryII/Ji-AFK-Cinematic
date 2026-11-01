package com.ji.afkcinematic.mixin;

import com.ji.afkcinematic.cinematic.CinematicManager;
import com.ji.afkcinematic.cinematic.CinematicState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityRenderer.class)
public abstract class EntityVisibilityMixin {

    @Inject(method = "getBoundingBoxForCulling", at = @At("HEAD"), cancellable = true)
    private void jiAfk$expandCullingBoxInCinematic(Entity entity, CallbackInfoReturnable<AABB> cir) {
        if (CinematicManager.getState() == CinematicState.CINEMATIC_ACTIVE) {
            Minecraft client = Minecraft.getInstance();
            if (client.player != null && entity == client.player) {
                cir.setReturnValue(client.player.getBoundingBox().inflate(256.0));
            }
        }
    }
}
