package com.ji.afkcinematic.mixin;

import com.ji.afkcinematic.JiAFKCinematic;
import com.ji.afkcinematic.cinematic.CameraController;
import com.ji.afkcinematic.cinematic.CinematicManager;
import com.ji.afkcinematic.cinematic.CinematicState;
import net.minecraft.client.Camera;
import net.minecraft.world.phys.Vec3;
import net.minecraft.client.DeltaTracker;
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

    @Inject(method = "update", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;calculateFov(F)F"), require = 0)
    private void onCameraUpdate(DeltaTracker tickCounter, CallbackInfo ci) {
        CinematicState state = CinematicManager.getState();
        if (state == CinematicState.CINEMATIC_ACTIVE) {
            float tickDelta = tickCounter.getGameTimeDeltaPartialTick(true);
            int ticksLeft = CinematicManager.getTicksLeftInCurrentShot();
            int durationTicks = CinematicManager.getShotDurationTicks();
            if (durationTicks <= 0) return; // guard against div-by-zero if config mutated mid-shot

            float ticksPassed = durationTicks - ticksLeft;
            float frameProgress = (ticksPassed + tickDelta) / durationTicks;

            // Deliberately linear: one second of real time always advances the same
            // amount through the shot, regardless of its beginning or ending.
            CameraController.evaluateFrame(frameProgress, tickDelta);

            Vec3 pos = CameraController.getFramePos();
            float pitch = CameraController.getFramePitch();
            float yaw = CameraController.getFrameYaw();
            float roll = CameraController.getFrameRoll();

            // Anti-NaN: never push a non-finite transform into the vanilla Camera, which
            // would propagate to the view matrix and render a corrupt frame.
            if (!Float.isFinite(pitch) || !Float.isFinite(yaw) || !Float.isFinite(roll)
                || !Double.isFinite(pos.x) || !Double.isFinite(pos.y) || !Double.isFinite(pos.z)) {
                JiAFKCinematic.LOGGER.warn("Skipping non-finite camera frame (pos={}, pitch={}, yaw={})", pos, pitch, yaw);
                return;
            }

            this.setPosition(pos.x, pos.y, pos.z);
            this.setRotation(yaw, pitch);
            if (roll != 0.0f) {
                this.rotation.rotateZ(roll * 0.017453292F);
                this.forwards.set(0.0f, 0.0f, 1.0f).rotate(this.rotation);
                this.up.set(0.0f, 1.0f, 0.0f).rotate(this.rotation);
                this.left.set(1.0f, 0.0f, 0.0f).rotate(this.rotation);
            }
        }
    }

    @Inject(method = "isDetached", at = @At("HEAD"), cancellable = true, require = 0)
    private void overrideThirdPerson(CallbackInfoReturnable<Boolean> cir) {
        if (CinematicManager.getState() == CinematicState.CINEMATIC_ACTIVE) {
            cir.setReturnValue(true);
        }
    }
}
