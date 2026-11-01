package com.ji.afkcinematic.mixin;

import com.ji.afkcinematic.render.HUDController;
import net.minecraft.client.gui.components.toasts.ToastManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Toasts render outside Gui/Hud. Omit only their drawing while we own HUD visibility.
 * Vanilla keeps updating its queue and tutorial state; nothing is cleared or disabled. */
@Mixin(ToastManager.class)
public abstract class ToastMixin {
    @Inject(method = "render(Lnet/minecraft/client/gui/GuiGraphics;)V",
            at = @At("HEAD"), cancellable = true, require = 1)
    private void jiAfk$hideCinematicToasts(CallbackInfo ci) {
        if (HUDController.isSuppressed()) ci.cancel();
    }
}
