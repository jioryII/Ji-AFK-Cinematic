package com.ji.afkcinematic.cinematic;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

public class CameraCollisionHelper {

    public static Vec3d resolveCollision(Vec3d startPos, Vec3d targetPos) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || client.player == null) {
            return targetPos;
        }

        // Perform raycast exactly at the current frame positions
        RaycastContext context = new RaycastContext(
            startPos,
            targetPos,
            RaycastContext.ShapeType.COLLIDER,
            RaycastContext.FluidHandling.NONE,
            client.player
        );

        HitResult hit = client.world.raycast(context);

        if (hit.getType() != HitResult.Type.MISS) {
            Vec3d hitPos = hit.getPos();
            Vec3d direction = startPos.subtract(hitPos).normalize();
            // Pull camera slightly forward to avoid clipping inside the block face
            return hitPos.add(direction.multiply(0.25));
        }

        return targetPos;
    }
}
