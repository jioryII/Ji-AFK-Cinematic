package com.ji.afkcinematic.mixin;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.concurrent.CompletableFuture;
@Mixin(Minecraft.class)
public class ResourceLayoutMixin {
    @Inject(method="reloadResourcePacks",at=@At("RETURN"),require=0)
    private void jiAfk$invalidateLayout(CallbackInfoReturnable<CompletableFuture<Void>> cir) {
        if(cir.getReturnValue()!=null)cir.getReturnValue().thenRun(com.ji.afkcinematic.diagnostic.RuntimeResilience::invalidateLayout);
    }
}
