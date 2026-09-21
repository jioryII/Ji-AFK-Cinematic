package com.ji.afkcinematic.mixin;

import com.ji.afkcinematic.cinematic.CinematicManager;
import com.ji.afkcinematic.cinematic.CinematicState;
import com.ji.afkcinematic.render.BedButtonAccess;
import com.ji.afkcinematic.config.ConfigManager;
import com.ji.afkcinematic.config.SleepLetterboxMode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.InBedChatScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InBedChatScreen.class)
public class InBedScreenMixin implements BedButtonAccess {
    @Shadow private Button leaveBedButton;

    @Inject(method = "init", at = @At("TAIL"))
    private void jiAfk$hideLeaveBed(CallbackInfo ci) { jiAfk$updateLeaveBedButton(); }

    @Override public void jiAfk$updateLeaveBedButton() {
        if (leaveBedButton == null) return;
        boolean cinematic = CinematicManager.getState() != CinematicState.IDLE;
        boolean sleepBars = Minecraft.getInstance().player != null
                && Minecraft.getInstance().player.isSleeping()
                && ConfigManager.getConfig().sleepLetterboxMode != SleepLetterboxMode.DISABLED;
        leaveBedButton.visible = !cinematic && !sleepBars;
        leaveBedButton.active = !cinematic && !sleepBars;
    }

}
