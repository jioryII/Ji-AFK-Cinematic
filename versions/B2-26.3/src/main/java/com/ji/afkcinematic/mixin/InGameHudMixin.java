package com.ji.afkcinematic.mixin;

import com.ji.afkcinematic.render.HUDController;
import com.ji.afkcinematic.render.LetterboxRenderer;
import com.ji.afkcinematic.render.CinematicHUDManager;
import com.ji.afkcinematic.config.ConfigManager;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Hides the in-game HUD orchestrator during a cinematic (hotbar, crosshair,
 * status bars, chat, plus all mod-registered HUD elements such as minimaps).
 *
 * 26.x ships a layered HUD pipeline under {@link Gui} (Mojmap: was InGameHud).
 * Cancelling its top-level {@code extractRenderState} suppresses both vanilla
 * UI and elements registered via Fabric's HudElementRegistry. Manual letterbox
 * drawing is preserved inside the cancelled method so the cinematics letterbox
 * stays on screen.
 *
 * Two signatures are targeted because Mojang changed the method signature
 * between 26.1.2 and 26.2: the 26.2 variant no longer receives a
 * {@link GuiGraphicsExtractor} parameter, so it is reconstructed via reflection
 * from {@code GameRenderer.gameRenderState.guiRenderState}.
 */
@Mixin(Gui.class)
public abstract class InGameHudMixin {
    @Invoker("extractChat")
    protected abstract void jiAfk$extractChat(GuiGraphicsExtractor context, DeltaTracker deltaTracker);

    @Inject(
        method = "extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/DeltaTracker;)V",
        at = @At("HEAD"),
        cancellable = true,
        require = 0
    )
    private void jiAfk$hideHudDuringCinematic(GuiGraphicsExtractor context, DeltaTracker deltaTracker, CallbackInfo ci) {
        if (HUDController.isHidden()) {
            LetterboxRenderer.renderFromHud(context, deltaTracker.getGameTimeDeltaPartialTick(true));
            if (CinematicHUDManager.shouldRenderPassiveChat(ConfigManager.getConfig())) {
                jiAfk$extractChat(context, deltaTracker);
            }
            ci.cancel();
        }
    }

}
