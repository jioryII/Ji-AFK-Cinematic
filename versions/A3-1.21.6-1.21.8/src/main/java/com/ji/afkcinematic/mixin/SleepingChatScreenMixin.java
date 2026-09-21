package com.ji.afkcinematic.mixin;

import com.ji.afkcinematic.cinematic.CinematicManager;
import com.ji.afkcinematic.cinematic.CinematicState;
import com.ji.afkcinematic.config.ConfigManager;
import com.ji.afkcinematic.config.SleepLetterboxMode;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.SleepingChatScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SleepingChatScreen.class)
public class SleepingChatScreenMixin {
    @Shadow private ButtonWidget stopSleepingButton;

    @Inject(method = "init", at = @At("TAIL"))
    private void jiAfk$init(CallbackInfo ci) { jiAfk$updateButton(); }

    @Inject(method = "render", at = @At("HEAD"))
    private void jiAfk$render(DrawContext context, int mouseX, int mouseY,
                              float delta, CallbackInfo ci) { jiAfk$updateButton(); }

    private void jiAfk$updateButton() {
        if (stopSleepingButton == null) return;
        boolean cinematic = CinematicManager.getState() != CinematicState.IDLE;
        boolean sleepBars = MinecraftClient.getInstance().player != null
                && MinecraftClient.getInstance().player.isSleeping()
                && ConfigManager.getConfig().sleepLetterboxMode != SleepLetterboxMode.DISABLED;
        stopSleepingButton.visible = !cinematic && !sleepBars;
        stopSleepingButton.active = !cinematic && !sleepBars;
    }
}
