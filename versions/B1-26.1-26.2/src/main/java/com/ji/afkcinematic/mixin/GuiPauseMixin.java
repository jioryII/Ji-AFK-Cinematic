package com.ji.afkcinematic.mixin;

import com.ji.afkcinematic.cinematic.CinematicManager;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Catches both vanilla and controller providers that request the pause menu. */
@Mixin(Gui.class)
public class GuiPauseMixin {
    @Inject(method = "setPauseScreen", at = @At("HEAD"), cancellable = true)
    private void jiAfk$consumePause(boolean fromEscape, boolean integratedServer, CallbackInfo ci) {
        if (CinematicManager.cancelForPause()) ci.cancel();
    }

    @Inject(method = "setScreen", at = @At("HEAD"), cancellable = true)
    private void jiAfk$consumeDirectPause(Screen screen, CallbackInfo ci) {
        if (screen instanceof PauseScreen && CinematicManager.cancelForPause()) ci.cancel();
    }
}
