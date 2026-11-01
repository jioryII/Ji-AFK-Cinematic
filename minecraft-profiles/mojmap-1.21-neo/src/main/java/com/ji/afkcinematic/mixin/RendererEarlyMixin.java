package com.ji.afkcinematic.mixin;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(value=GameRenderer.class, priority=500)
public class RendererEarlyMixin {
    @Inject(method="render", at=@At("HEAD"), require=1)
    private void jiAfk$frame(CallbackInfo ci) { com.ji.afkcinematic.diagnostic.RuntimeResilience.beginRender(); }
}
