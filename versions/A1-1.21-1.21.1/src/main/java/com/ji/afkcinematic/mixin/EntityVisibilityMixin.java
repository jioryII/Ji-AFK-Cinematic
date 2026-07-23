package com.ji.afkcinematic.mixin;

import com.ji.afkcinematic.cinematic.CinematicManager;
import com.ji.afkcinematic.cinematic.CinematicState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Box;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityVisibilityMixin {

    @Inject(method = "getVisibilityBoundingBox", at = @At("HEAD"), cancellable = true, require = 0)
    private void expandVisibilityBoxInCinematic(CallbackInfoReturnable<Box> cir) {
        if (CinematicManager.getState() == CinematicState.CINEMATIC_ACTIVE) {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player != null && (Object) this == client.player) {
                // Return a massive bounding box so the player is never culled by section or frustum
                cir.setReturnValue(client.player.getBoundingBox().expand(256.0));
            }
        }
    }
}
