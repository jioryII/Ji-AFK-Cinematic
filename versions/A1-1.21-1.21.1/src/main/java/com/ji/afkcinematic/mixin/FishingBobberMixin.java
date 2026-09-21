package com.ji.afkcinematic.mixin;

import com.ji.afkcinematic.input.FishingBiteAccess;
import net.minecraft.entity.projectile.FishingBobberEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(FishingBobberEntity.class)
public class FishingBobberMixin implements FishingBiteAccess {
    @Shadow private boolean caughtFish;

    @Override public boolean jiAfk$isBiting() { return caughtFish; }
}
