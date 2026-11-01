package com.ji.afkcinematic.mixin;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
/** Applied only to 26.2, whose GUI state exposes the HUD visibility flag. */
@Mixin(GuiRenderState.class)
public interface DedicatedHudStateAccess {
    @Accessor(value="isHudHidden", remap=false) void jiAfk$hidden(boolean hidden);
}
