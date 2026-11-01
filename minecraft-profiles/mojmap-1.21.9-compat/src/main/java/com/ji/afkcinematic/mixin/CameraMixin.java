package com.ji.afkcinematic.mixin;

import com.ji.afkcinematic.diagnostic.HookCapabilities;
import com.ji.afkcinematic.diagnostic.DiagnosticReport.Hook;

import com.ji.afkcinematic.cinematic.CameraController;
import com.ji.afkcinematic.cinematic.CameraFrame;
import com.ji.afkcinematic.cinematic.CinematicManager;
import com.ji.afkcinematic.cinematic.CinematicState;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Camera.class)
public abstract class CameraMixin {
    @Shadow @Final private Quaternionf rotation;
    @Shadow @Final private Vector3f forwards;
    @Shadow @Final private Vector3f up;
    @Shadow @Final private Vector3f left;

    @Shadow
    protected abstract void setPosition(double x, double y, double z);

    @Shadow
    protected abstract void setRotation(float yaw, float pitch);

    @Inject(method = "setup", at = @At("HEAD"), order = 500)
    private void beforeCameraUpdate(BlockGetter level, Entity focusedEntity, boolean detached,
                                    boolean mirrored, float tickDelta, CallbackInfo ci) {
        com.ji.afkcinematic.compat.SmoothF5Compatibility.onCameraUpdate(this);
    }

    @Inject(method = "setup", at = @At("TAIL"), order = 2000)
    private void onCameraUpdate(BlockGetter level, Entity focusedEntity, boolean detached,
                                boolean mirrored, float tickDelta, CallbackInfo ci) {
        HookCapabilities.observed(Hook.CAMERA);
        CinematicState state = CinematicManager.getState();
        if (state == CinematicState.CINEMATIC_ACTIVE) {
            int ticksLeft = CinematicManager.getTicksLeftInCurrentShot();
            int durationTicks = CinematicManager.getShotDurationTicks();
            if (durationTicks <= 0) return;
            float frameProgress = (durationTicks - ticksLeft + tickDelta) / durationTicks;
            CameraFrame frame = CameraController.evaluateFrame(frameProgress, tickDelta);
            // Null preserves the vanilla transform when no valid cinematic frame is available.
            if (frame == null) return;
            this.setPosition(frame.x(), frame.y(), frame.z());
            this.setRotation(frame.yaw(), frame.pitch());
            float roll = frame.roll();
            if (roll != 0.0f) {
                this.rotation.rotateZ(roll * 0.017453292F);
                this.forwards.set(0.0f, 0.0f, 1.0f).rotate(this.rotation);
                this.up.set(0.0f, 1.0f, 0.0f).rotate(this.rotation);
                this.left.set(1.0f, 0.0f, 0.0f).rotate(this.rotation);
            }
            com.ji.afkcinematic.diagnostic.RuntimeResilience.applied(frame.x(), frame.y(), frame.z(),
                    rotation.x(), rotation.y(), rotation.z(), rotation.w());
        }
    }

    @Inject(method = "isDetached", at = @At("HEAD"), cancellable = true, require = 0)
    private void overrideThirdPerson(CallbackInfoReturnable<Boolean> cir) {
        if (CinematicManager.getState() != CinematicState.IDLE) {
            cir.setReturnValue(true);
        }
    }
}
