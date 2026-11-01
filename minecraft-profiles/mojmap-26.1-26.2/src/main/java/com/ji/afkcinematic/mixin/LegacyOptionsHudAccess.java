package com.ji.afkcinematic.mixin;
import net.minecraft.client.Options;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
/** Applied only to 26.1.x by the mixin plugin. */
@Mixin(Options.class)
public interface LegacyOptionsHudAccess {
    @Accessor(value="hideGui", remap=false) boolean jiAfk$hidden();
    @Accessor(value="hideGui", remap=false) void jiAfk$hidden(boolean hidden);
}
