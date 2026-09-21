package com.ji.afkcinematic.cinematic;

import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.ClipContext;

public class CameraCollisionHelper {

    public static Vec3 resolveCollision(Vec3 startPos, Vec3 targetPos) {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null || client.player == null) {
            return targetPos;
        }

        // Perform raycast exactly at the current frame positions
        net.minecraft.world.level.ClipContext context = new net.minecraft.world.level.ClipContext(
            startPos,
            targetPos,
            net.minecraft.world.level.ClipContext.Block.COLLIDER,
            net.minecraft.world.level.ClipContext.Fluid.NONE,
            client.player
        );

        HitResult hit = client.level.clip(context);

        if (hit.getType() != HitResult.Type.MISS) {
            Vec3 hitPos = hit.getLocation();
            Vec3 direction = startPos.subtract(hitPos).normalize();
            // Pull camera slightly forward to avoid clipping inside the block face
            return hitPos.add(direction.scale(0.25));
        }

        return targetPos;
    }
}
