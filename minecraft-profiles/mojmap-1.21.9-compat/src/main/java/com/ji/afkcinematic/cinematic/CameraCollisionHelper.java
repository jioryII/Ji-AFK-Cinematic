package com.ji.afkcinematic.cinematic;

import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.ClipContext;
import net.minecraft.core.BlockPos;
import com.ji.afkcinematic.cinematic.director.CameraVector;
import com.ji.afkcinematic.cinematic.director.CompositionDirector;

public class CameraCollisionHelper {

    public static Vec3 resolveCollision(Vec3 startPos, Vec3 targetPos) {
        return DirectedCameraShot.nativeVector(CompositionDirector.resolveCollision(
                DirectedCameraShot.vector(startPos), DirectedCameraShot.vector(targetPos),
                CameraCollisionHelper::clearDistance));
    }

    /** Read-only query. Unloaded terrain is unknown, never treated as open space. */
    public static double clearDistance(CameraVector origin, CameraVector direction, double distance) {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null || client.player == null || !origin.finite() || !direction.finite()
                || !Double.isFinite(distance) || distance < 0) return 0;
        Vec3 startPos = DirectedCameraShot.nativeVector(origin);
        Vec3 targetPos = DirectedCameraShot.nativeVector(origin.add(direction.scale(distance)));
        if (!client.level.hasChunkAt(BlockPos.containing(startPos))
                || !client.level.hasChunkAt(BlockPos.containing(targetPos))) return 0;
        // Above water (including boats), water is a camera boundary. An underwater
        // player keeps ordinary underwater takes; no player-to-camera tether is traced.
        boolean surfaceView = !client.player.isUnderWater();
        BlockPos startBlock = BlockPos.containing(startPos);
        var fluid = client.level.getFluidState(startBlock);
        if (surfaceView && fluid.is(net.minecraft.tags.FluidTags.WATER)
                && startPos.y < startBlock.getY() + fluid.getHeight(client.level, startBlock) + .12) return 0;
        ClipContext context = new ClipContext(startPos, targetPos, ClipContext.Block.COLLIDER,
                surfaceView ? ClipContext.Fluid.ANY : ClipContext.Fluid.NONE, client.player);
        HitResult hit = client.level.clip(context);
        if (surfaceView && hit.getType() == HitResult.Type.BLOCK
                && hit instanceof net.minecraft.world.phys.BlockHitResult blockHit
                && !client.level.getFluidState(blockHit.getBlockPos()).isEmpty()
                && !client.level.getFluidState(blockHit.getBlockPos()).is(net.minecraft.tags.FluidTags.WATER)) {
            // Lava and other fluid types keep their existing solid-block behavior.
            hit = client.level.clip(new ClipContext(startPos, targetPos, ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE, client.player));
        }
        double clear = hit.getType() == HitResult.Type.MISS ? distance : startPos.distanceTo(hit.getLocation());
        if (distance <= 1.7) {
            var area = new net.minecraft.world.phys.AABB(startPos, targetPos).inflate(.15);
            for (var entity : client.level.getEntities(client.player, area, e -> e.isPickable() && !e.isSpectator())) {
                var bounds = entity.getBoundingBox().inflate(.12);
                if (bounds.contains(startPos)) continue; // Never jump when an animal overlaps the camera.
                var intersection = bounds.clip(startPos, targetPos);
                if (intersection.isPresent()) clear = Math.min(clear, startPos.distanceTo(intersection.get()));
            }
        }
        return clear;
    }
}
