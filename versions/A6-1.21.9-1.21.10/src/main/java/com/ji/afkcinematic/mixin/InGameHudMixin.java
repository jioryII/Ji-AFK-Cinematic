package com.ji.afkcinematic.mixin;

import com.ji.afkcinematic.render.HUDController;
import com.ji.afkcinematic.render.LetterboxRenderer;
import net.minecraft.client.gui.hud.InGameHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;

@Mixin(InGameHud.class)
public class InGameHudMixin {
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void onRender(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        if (HUDController.isHidden()) {
            // Manually render our black bars since we are aborting the rest of the HUD rendering
            LetterboxRenderer.renderFromHud(context, tickCounter.getTickProgress(false));
            ci.cancel();
        }
    }
}
