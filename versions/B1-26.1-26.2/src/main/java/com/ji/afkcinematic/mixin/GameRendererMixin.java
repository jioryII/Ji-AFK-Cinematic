package com.ji.afkcinematic.mixin;

import com.ji.afkcinematic.render.HUDController;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Explicitly cancels the first-person hand item render while the cinematic HUD
 * is hidden. Without this, the hand sprite is drawn independently of the HUD
 * pipeline on 26.x.
 */
@Mixin(value = GameRenderer.class)
public class GameRendererMixin {

    @Inject(method = "renderItemInHand", at = @At("HEAD"), cancellable = true, require = 0)
    private void jiAfk$hideHandDuringCinematic(CallbackInfo ci) {
        if (HUDController.isHidden()) {
            ci.cancel();
        }
    }
}
