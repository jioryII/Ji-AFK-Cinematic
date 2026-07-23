package com.ji.afkcinematic.mixin;

import com.ji.afkcinematic.cinematic.CameraController;
import com.ji.afkcinematic.cinematic.CinematicManager;
import com.ji.afkcinematic.cinematic.CinematicState;
import net.minecraft.client.Camera;
import net.minecraft.world.phys.Vec3;
import net.minecraft.client.DeltaTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Camera.class)
public abstract class CameraMixin {
    @Shadow
    protected abstract void setPosition(double x, double y, double z);

    @Shadow
    protected abstract void setRotation(float yaw, float pitch);

    @Inject(method = "update", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;calculateFov(F)F"), require = 0)
    private void onCameraUpdate(DeltaTracker tickCounter, CallbackInfo ci) {
        CinematicState state = CinematicManager.getState();
        if (state == CinematicState.CINEMATIC_ACTIVE) {
            float tickDelta = tickCounter.getGameTimeDeltaPartialTick(true);
            int ticksLeft = CinematicManager.getTicksLeftInCurrentShot();
            int durationTicks = CinematicManager.getShotDurationTicks();
            float ticksPassed = durationTicks - ticksLeft;
            float frameProgress = (ticksPassed + tickDelta) / durationTicks;

            CameraController.evaluateFrame(frameProgress, tickDelta);

            Vec3 pos = CameraController.getFramePos();
            float pitch = CameraController.getFramePitch();
            float yaw = CameraController.getFrameYaw();

            this.setPosition(pos.x, pos.y, pos.z);
            this.setRotation(yaw, pitch);
        }
    }

    @Inject(method = "isDetached", at = @At("HEAD"), cancellable = true, require = 0)
    private void overrideThirdPerson(CallbackInfoReturnable<Boolean> cir) {
        if (CinematicManager.getState() == CinematicState.CINEMATIC_ACTIVE) {
            cir.setReturnValue(true);
        }
    }
}
