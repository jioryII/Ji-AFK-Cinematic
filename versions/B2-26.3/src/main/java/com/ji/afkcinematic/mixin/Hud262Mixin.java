package com.ji.afkcinematic.mixin;

import com.ji.afkcinematic.config.ConfigManager;
import com.ji.afkcinematic.config.SleepLetterboxMode;
import com.ji.afkcinematic.render.CinematicHUDManager;
import com.ji.afkcinematic.render.HUDController;
import com.ji.afkcinematic.render.LetterboxRenderer;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 26.2+ HUD-only hook; cancelling Gui itself would also suppress screens such as chat. */
@Mixin(targets = "net.minecraft.client.gui.Hud")
public abstract class Hud262Mixin {
    @Invoker("extractChat")
    protected abstract void jiAfk$extractChat(GuiGraphicsExtractor context, DeltaTracker deltaTracker);

    @Inject(method = "extractRenderState", at = @At("HEAD"), cancellable = true, require = 0)
    private void jiAfk$hideHudDuringCinematic(GuiGraphicsExtractor context, DeltaTracker deltaTracker, CallbackInfo ci) {
        boolean sleepBars = LetterboxRenderer.isSleepActive();
        if (!HUDController.isHidden() && !sleepBars) return;

        LetterboxRenderer.renderFromHud(context, deltaTracker.getGameTimeDeltaPartialTick(true));
        // Chat, the bed controls and any subsequently extracted screen stay above
        // the bars while HUD elements and minimap overlays are omitted.
        context.nextStratum();
        boolean moderateSleep = sleepBars
                && ConfigManager.getConfig().sleepLetterboxMode == SleepLetterboxMode.MODERATE;
        if (moderateSleep || !sleepBars
                && CinematicHUDManager.shouldRenderPassiveChat(ConfigManager.getConfig())) {
            jiAfk$extractChat(context, deltaTracker);
        }
        ci.cancel();
    }
}
