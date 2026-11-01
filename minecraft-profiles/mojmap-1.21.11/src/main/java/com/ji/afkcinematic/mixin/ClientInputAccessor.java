package com.ji.afkcinematic.mixin;

import net.minecraft.client.player.ClientInput;
import net.minecraft.world.phys.Vec2;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Exact SDK access to the protected processed movement vector. */
@Mixin(ClientInput.class)
public interface ClientInputAccessor {
    @Accessor("moveVector")
    void jiAfk$setMoveVector(Vec2 value);
}
