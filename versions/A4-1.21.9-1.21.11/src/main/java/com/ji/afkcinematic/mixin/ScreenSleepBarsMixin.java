package com.ji.afkcinematic.mixin;

import com.ji.afkcinematic.render.LetterboxRenderer;
import com.ji.afkcinematic.config.ConfigManager;
import com.ji.afkcinematic.config.SleepLetterboxMode;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Render sleep bars above the bed screen and replacement screens from sleep mods. */
@Mixin(Screen.class)
public class ScreenSleepBarsMixin {
    @Inject(method = "render", at = @At("HEAD"), cancellable = true, require = 0)
    private void jiAfk$renderSleepBars(DrawContext context, int mouseX, int mouseY,
                                       float delta, CallbackInfo ci) {
        if (LetterboxRenderer.isSleepActive()
                && ConfigManager.getConfig().sleepLetterboxMode == SleepLetterboxMode.COMPLETE) {
            ci.cancel();
        }
    }
}
