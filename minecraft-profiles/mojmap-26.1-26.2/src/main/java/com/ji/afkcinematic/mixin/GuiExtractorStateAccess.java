package com.ji.afkcinematic.mixin;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
/** Fabric widens this field; a real accessor also supports the native NeoForge API. */
@Mixin(GuiGraphicsExtractor.class)
public interface GuiExtractorStateAccess {
    @Accessor(value="guiRenderState", remap=false) GuiRenderState jiAfk$state();
}
