package com.ji.afkcinematic.mixin;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(Camera.class)
public class CameraConsumptionMixin {
    @Inject(method="extractRenderState",at=@At("TAIL"),order=2000,require=0)
    private void jiAfk$consumeCamera(CameraRenderState state, float partialTick, CallbackInfo ci) {
        if(!state.initialized || state.pos==null || state.orientation==null)return;
        var p=state.pos;var q=state.orientation;
        com.ji.afkcinematic.diagnostic.RuntimeResilience.consumed(p.x,p.y,p.z,q.x(),q.y(),q.z(),q.w());
    }
}
