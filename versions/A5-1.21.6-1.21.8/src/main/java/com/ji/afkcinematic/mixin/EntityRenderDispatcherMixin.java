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

@Mixin(EntityRenderer.class)
public class EntityRenderDispatcherMixin {

    @Inject(method = "shouldRender", at = @At("HEAD"), cancellable = true)
    private <E extends Entity> void forceRenderPlayer(E entity, Frustum frustum, double x, double y, double z, CallbackInfoReturnable<Boolean> cir) {
        if (CinematicManager.getState() == CinematicState.CINEMATIC_ACTIVE) {
            MinecraftClient client = MinecraftClient.getInstance();
            if (entity == client.player) {
                cir.setReturnValue(true);
            }
        }
    }
}
