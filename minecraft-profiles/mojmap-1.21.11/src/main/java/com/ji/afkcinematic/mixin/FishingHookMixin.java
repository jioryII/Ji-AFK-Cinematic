package com.ji.afkcinematic.mixin;

import com.ji.afkcinematic.input.FishingBiteAccess;
import net.minecraft.world.entity.projectile.FishingHook;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(FishingHook.class)
public class FishingHookMixin implements FishingBiteAccess {
    @Shadow private boolean biting;

    @Override public boolean jiAfk$isBiting() { return biting; }
}
