package com.ji.afkcinematic.mixin;

import com.ji.afkcinematic.render.BedButtonAccess;
import com.ji.afkcinematic.render.LetterboxRenderer;
import com.ji.afkcinematic.config.ConfigManager;
import com.ji.afkcinematic.config.SleepLetterboxMode;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public class ScreenSleepBarsMixin {
    @Inject(method = "renderWithTooltip", at = @At("HEAD"), cancellable = true)
    private void jiAfk$updateBedButton(GuiGraphics graphics, int mouseX, int mouseY,
                                       float partialTick, CallbackInfo ci) {
        if ((Object) this instanceof BedButtonAccess bed) bed.jiAfk$updateLeaveBedButton();
        if (LetterboxRenderer.isSleepActive()
                && (Object) this instanceof net.minecraft.client.gui.screens.InBedChatScreen
                && !com.ji.afkcinematic.render.CinematicHUDManager.shouldRenderPassiveChat(ConfigManager.getConfig())) {
            ci.cancel();
        }
    }
}
