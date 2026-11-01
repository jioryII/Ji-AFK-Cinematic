package com.ji.afkcinematic.mixin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
/** Hidden-chat cinematics retain typing, without exposing messages or recalled history. */
@Mixin(net.minecraft.client.gui.screens.ChatScreen.class)
public abstract class ChatHistoryMixin {
    @Inject(method="moveInHistory",at=@At("HEAD"),cancellable=true,require=1)
    private void jiAfk$hideHistory(CallbackInfo ci) {
        if (com.ji.afkcinematic.render.HUDController.isSuppressed()
                && com.ji.afkcinematic.config.ConfigManager.getConfig().chatVisibility == com.ji.afkcinematic.config.CinematicChatVisibility.HIDDEN) ci.cancel();
    }
}
