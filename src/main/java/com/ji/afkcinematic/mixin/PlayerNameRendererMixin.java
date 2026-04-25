package com.ji.afkcinematic.mixin;

import com.ji.afkcinematic.cinematic.CinematicManager;
import com.ji.afkcinematic.cinematic.CinematicState;
import com.ji.afkcinematic.config.ConfigManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityRenderer.class)
public abstract class PlayerNameRendererMixin<T extends Entity> {

    @Inject(method = "hasLabel", at = @At("HEAD"), cancellable = true)
    private void forceShowLocalPlayerName(T entity, double distance, CallbackInfoReturnable<Boolean> cir) {
        if (CinematicManager.getState() == CinematicState.CINEMATIC_ACTIVE
                && ConfigManager.getConfig().showPlayerName
                && entity == MinecraftClient.getInstance().player) {
            cir.setReturnValue(true);
        }
    }
}

