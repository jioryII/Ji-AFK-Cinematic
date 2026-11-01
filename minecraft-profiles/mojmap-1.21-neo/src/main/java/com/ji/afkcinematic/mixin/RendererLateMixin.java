package com.ji.afkcinematic.mixin;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(value=GameRenderer.class, priority=2000)
public class RendererLateMixin {
    @Inject(method="render", at=@At("RETURN"), require=1)
    private void jiAfk$frame(CallbackInfo ci) { com.ji.afkcinematic.diagnostic.RuntimeResilience.endRender(); }
}
