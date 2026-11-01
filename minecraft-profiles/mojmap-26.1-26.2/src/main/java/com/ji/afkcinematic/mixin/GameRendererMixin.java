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
    // 26.x updates and extracts the camera before render(), in separate calls.
    @Inject(method = "update", at = @At("HEAD"), order = 500, require = 1)
    private void jiAfk$resilienceFrameStart(CallbackInfo ci) { com.ji.afkcinematic.diagnostic.RuntimeResilience.beginRender(); }
    @Inject(method = "extract", at = @At("RETURN"), order = 2000, require = 1)
    private void jiAfk$resilienceFrameEnd(CallbackInfo ci) { com.ji.afkcinematic.diagnostic.RuntimeResilience.endRender(); }


    @Inject(method={"extract", "render"}, at=@At("HEAD"), require=1)
    private void jiAfk$beginHudFrame(CallbackInfo ci) { HUDController.beginFrame(); }

    @Inject(method={"extract", "render"}, at=@At("RETURN"), require=1)
    private void jiAfk$endHudFrame(CallbackInfo ci) { HUDController.endFrame(); }

    @Inject(method = "renderItemInHand", at = @At("HEAD"), cancellable = true, require = 0)
    private void jiAfk$hideHandDuringCinematic(CallbackInfo ci) {
        if (HUDController.isHidden()) {
            ci.cancel();
        }
    }
}
