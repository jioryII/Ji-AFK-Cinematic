package com.ji.afkcinematic.mixin;

import com.ji.afkcinematic.cinematic.CinematicManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 26.1 pause path; 26.2 moved these responsibilities to Gui. */
@Mixin(Minecraft.class)
public class MinecraftPauseMixin {
    @Inject(method = "pauseGame", at = @At("HEAD"), cancellable = true, require = 0)
    private void jiAfk$consumePause(boolean integratedServer, CallbackInfo ci) {
        if (CinematicManager.cancelForPause()) ci.cancel();
    }

    @Inject(method = "setScreen", at = @At("HEAD"), cancellable = true, require = 0)
    private void jiAfk$consumeDirectPause(Screen screen, CallbackInfo ci) {
        if (screen instanceof PauseScreen && CinematicManager.cancelForPause()) ci.cancel();
    }
}
