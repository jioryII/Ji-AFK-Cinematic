package com.ji.afkcinematic.mixin;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(value=Camera.class, priority=500)
public class CameraEarlyMixin {
    @Inject(method="setup", at=@At("HEAD"))
    private void jiAfk$before(BlockGetter level, Entity entity, boolean detached, boolean mirrored, float delta, CallbackInfo ci) {
        com.ji.afkcinematic.compat.SmoothF5Compatibility.onCameraUpdate(this);
    }
}
