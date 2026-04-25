package com.ji.afkcinematic.mixin;

import com.ji.afkcinematic.cinematic.CameraController;
import com.ji.afkcinematic.cinematic.CinematicManager;
import com.ji.afkcinematic.cinematic.CinematicState;
import net.minecraft.client.render.Camera;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class CameraMixin {
    @Shadow
    protected abstract void setPos(double x, double y, double z);

    @Shadow
    protected abstract void setRotation(float yaw, float pitch);

    @Inject(method = "update", at = @At("TAIL"))
    private void onCameraUpdate(net.minecraft.world.World area, net.minecraft.entity.Entity focusedEntity, boolean thirdPerson, boolean inverseView, float tickDelta, CallbackInfo ci) {
        CinematicState state = CinematicManager.getState();
        if (state == CinematicState.CINEMATIC_ACTIVE) {
            int ticksLeft = CinematicManager.getTicksLeftInCurrentShot();
            int durationTicks = CinematicManager.getShotDurationTicks();
            
            // Parametric evaluation: Sub-tick resolution is passed directly to the cinematic formulas
            // This achieves perfectly smooth frame rates without 20fps tick snapping
            float ticksPassed = durationTicks - ticksLeft;
            float frameProgress = (ticksPassed + tickDelta) / durationTicks;

            CameraController.evaluateFrame(frameProgress, tickDelta);

            Vec3d pos = CameraController.getFramePos();
            float pitch = CameraController.getFramePitch();
            float yaw = CameraController.getFrameYaw();

            this.setPos(pos.x, pos.y, pos.z);
            this.setRotation(yaw, pitch);
        }
    }
}
