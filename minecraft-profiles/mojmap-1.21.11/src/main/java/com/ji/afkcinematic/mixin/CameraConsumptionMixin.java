package com.ji.afkcinematic.mixin;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(GameRenderer.class)
public class CameraConsumptionMixin {
    @Inject(method="extractCamera",at=@At("TAIL"),order=2000,require=0)
    private void jiAfk$consumeCamera(CallbackInfo ci) {
        var camera=Minecraft.getInstance().gameRenderer.getMainCamera();
        var p=camera.position();var q=camera.rotation();
        com.ji.afkcinematic.diagnostic.RuntimeResilience.consumed(p.x,p.y,p.z,q.x(),q.y(),q.z(),q.w());
    }
}
