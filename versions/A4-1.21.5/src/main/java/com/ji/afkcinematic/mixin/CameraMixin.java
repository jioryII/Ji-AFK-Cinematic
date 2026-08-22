package com.ji.afkcinematic.mixin;

import com.ji.afkcinematic.JiAFKCinematic;
import com.ji.afkcinematic.cinematic.CameraController;
import com.ji.afkcinematic.cinematic.CinematicManager;
import com.ji.afkcinematic.cinematic.CinematicState;
import net.minecraft.client.render.Camera;
import net.minecraft.util.math.Vec3d;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class CameraMixin {
    @Shadow @Final private static Vector3f HORIZONTAL;
    @Shadow @Final private static Vector3f VERTICAL;
    @Shadow @Final private static Vector3f DIAGONAL;
    @Shadow @Final private Quaternionf rotation;
    @Shadow @Final private Vector3f horizontalPlane;
    @Shadow @Final private Vector3f verticalPlane;
    @Shadow @Final private Vector3f diagonalPlane;

    @Shadow
    protected abstract void setPos(double x, double y, double z);

    @Shadow
    protected abstract void setRotation(float yaw, float pitch);

    @Inject(method = "update", at = @At("TAIL"), require = 0)
    private void onCameraUpdate(net.minecraft.world.BlockView area, net.minecraft.entity.Entity focusedEntity, boolean thirdPerson, boolean inverseView, float tickDelta, CallbackInfo ci) {
        CinematicState state = CinematicManager.getState();
        if (state == CinematicState.CINEMATIC_ACTIVE) {
            int ticksLeft = CinematicManager.getTicksLeftInCurrentShot();
            int durationTicks = CinematicManager.getShotDurationTicks();
            if (durationTicks <= 0) return; // guard against div-by-zero if config mutated mid-shot

            // Parametric evaluation: Sub-tick resolution is passed directly to the cinematic formulas
            // This achieves perfectly smooth frame rates without 20fps tick snapping
            float ticksPassed = durationTicks - ticksLeft;
            float frameProgress = (ticksPassed + tickDelta) / durationTicks;

            // Deliberately linear: one second of real time always advances the same
            // amount through the shot, regardless of its beginning or ending.
            CameraController.evaluateFrame(frameProgress, tickDelta);

            Vec3d pos = CameraController.getFramePos();
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

            this.setPos(pos.x, pos.y, pos.z);
            this.setRotation(yaw, pitch);
            if (roll != 0.0f) {
                this.rotation.rotateZ(roll * 0.017453292F);
                HORIZONTAL.rotate(this.rotation, this.horizontalPlane);
                VERTICAL.rotate(this.rotation, this.verticalPlane);
                DIAGONAL.rotate(this.rotation, this.diagonalPlane);
            }
        }
    }

    @Inject(method = "isThirdPerson", at = @At("HEAD"), cancellable = true, require = 0)
    private void overrideThirdPerson(org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<Boolean> cir) {
        if (CinematicManager.getState() == CinematicState.CINEMATIC_ACTIVE) {
            cir.setReturnValue(true);
        }
    }
}
