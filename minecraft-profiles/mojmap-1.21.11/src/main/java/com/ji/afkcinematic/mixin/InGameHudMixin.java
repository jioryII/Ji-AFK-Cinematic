package com.ji.afkcinematic.mixin;

import com.ji.afkcinematic.diagnostic.HookCapabilities;
import com.ji.afkcinematic.diagnostic.DiagnosticReport.Hook;

import com.ji.afkcinematic.render.HUDController;
import com.ji.afkcinematic.render.LetterboxRenderer;
import com.ji.afkcinematic.render.CinematicHUDManager;
import com.ji.afkcinematic.config.ConfigManager;
import com.ji.afkcinematic.config.SleepLetterboxMode;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Hides the in-game HUD orchestrator during a cinematic (hotbar, crosshair,
 * status bars, chat, plus all mod-registered HUD elements such as minimaps).
 *
 * Minecraft 1.21.11 renders HUD through Gui.render. Sleep owns its overlay
 * even before the cinematic begins; Chat visible owns all chat passes.
 */
@Mixin(Gui.class)
public abstract class InGameHudMixin {
    @Invoker("renderChat")
    protected abstract void jiAfk$renderChat(GuiGraphics context, DeltaTracker deltaTracker);

    @Inject(
        method = "render(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/DeltaTracker;)V",
        at = @At("HEAD"),
        cancellable = true,
        require = 0
    )
    private void jiAfk$hideHudDuringCinematic(GuiGraphics context, DeltaTracker deltaTracker, CallbackInfo ci) {
        HookCapabilities.observed(Hook.HUD);
        boolean sleepBars = LetterboxRenderer.isSleepActive();
        if (!HUDController.isHidden() && !sleepBars) return;
        LetterboxRenderer.renderFromHud(context, deltaTracker.getGameTimeDeltaPartialTick(false));
        if (CinematicHUDManager.shouldRenderPassiveChat(ConfigManager.getConfig())) {
            jiAfk$renderChat(context, deltaTracker);
        }
        ci.cancel();
    }

}
