package com.ji.afkcinematic.mixin;

import com.ji.afkcinematic.render.HUDController;
import com.ji.afkcinematic.render.LetterboxRenderer;
import net.minecraft.client.gui.Gui;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.DeltaTracker;

@Mixin(Gui.class)
public class InGameHudMixin {
    @Inject(method = "extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/DeltaTracker;)V", at = @At("HEAD"), cancellable = true, require = 0)
    private void onRender(GuiGraphicsExtractor context, DeltaTracker tickCounter, CallbackInfo ci) {
        if (HUDController.isHidden()) {
            // Manually render our black bars since we are aborting the rest of the HUD rendering
            LetterboxRenderer.renderFromHud(context, tickCounter.getGameTimeDeltaTicks());
            ci.cancel();
        }
    }

    @ModifyVariable(
        method = "extractRenderState(Lnet/minecraft/client/DeltaTracker;ZZ)V",
        at = @At("HEAD"),
        argsOnly = true,
        ordinal = 1,
        require = 0
    )
    private boolean onExtractRenderState_ModifyRenderUi(boolean renderUi) {
        if (HUDController.isHidden()) {
            return false;
        }
        return renderUi;
    }
}
