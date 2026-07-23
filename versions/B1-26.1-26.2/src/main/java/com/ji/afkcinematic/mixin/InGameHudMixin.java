package com.ji.afkcinematic.mixin;

import com.ji.afkcinematic.render.HUDController;
import com.ji.afkcinematic.render.LetterboxRenderer;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

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
public class InGameHudMixin {

    private static java.lang.reflect.Field cachedGameRenderStateField = null;

    @Inject(
        method = "extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/DeltaTracker;)V",
        at = @At("HEAD"),
        cancellable = true,
        require = 0
    )
    private void jiAfk$hideHudDuringCinematic(GuiGraphicsExtractor context, DeltaTracker deltaTracker, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (com.ji.afkcinematic.ScreenHelper.getCurrentScreen(mc) != null) return;
        if (HUDController.isHidden()) {
            LetterboxRenderer.renderFromHud(context, deltaTracker.getGameTimeDeltaPartialTick(true));
            ci.cancel();
        }
    }

    @Inject(
        method = "extractRenderState(Lnet/minecraft/client/DeltaTracker;ZZ)V",
        at = @At("HEAD"),
        cancellable = true,
        require = 0
    )
    private void jiAfk$hideHudDuringCinematic262(DeltaTracker deltaTracker, boolean renderCrosshair, boolean renderChat, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (com.ji.afkcinematic.ScreenHelper.getCurrentScreen(mc) != null) return;
        if (HUDController.isHidden()) {
            try {
                if (cachedGameRenderStateField == null) {
                    for (java.lang.reflect.Field f : mc.gameRenderer.getClass().getDeclaredFields()) {
                        if (f.getType() == net.minecraft.client.renderer.state.GameRenderState.class) {
                            f.setAccessible(true);
                            cachedGameRenderStateField = f;
                            break;
                        }
                    }
                }

                if (cachedGameRenderStateField != null) {
                    net.minecraft.client.renderer.state.GameRenderState renderStateContainer =
                        (net.minecraft.client.renderer.state.GameRenderState) cachedGameRenderStateField.get(mc.gameRenderer);

                    if (renderStateContainer != null && renderStateContainer.guiRenderState != null) {
                        GuiGraphicsExtractor context = new GuiGraphicsExtractor(
                            mc,
                            renderStateContainer.guiRenderState,
                            mc.getWindow().getGuiScaledWidth(),
                            mc.getWindow().getGuiScaledHeight()
                        );
                        LetterboxRenderer.renderFromHud(context, deltaTracker.getGameTimeDeltaPartialTick(true));
                    }
                }
            } catch (Exception e) {
                com.ji.afkcinematic.JiAFKCinematic.LOGGER.warn("Failed to reconstruct GuiGraphicsExtractor for 26.2 letterbox fallback", e);
            }
            ci.cancel();
        }
    }
}
