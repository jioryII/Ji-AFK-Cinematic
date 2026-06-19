package com.ji.afkcinematic.mixin;

import com.ji.afkcinematic.cinematic.CinematicManager;
import com.ji.afkcinematic.cinematic.CinematicState;
import com.ji.afkcinematic.config.ConfigManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityRenderer.class)
public abstract class PlayerNameRendererMixin<T extends Entity> {

    @Inject(method = "shouldShowName", at = @At("HEAD"), cancellable = true)
    private void forceShowLocalPlayerName(T entity, double distance, CallbackInfoReturnable<Boolean> cir) {
        if (CinematicManager.getState() == CinematicState.CINEMATIC_ACTIVE
                
                && entity == Minecraft.getInstance().player) {
            cir.setReturnValue(true);
        }
    }
}

