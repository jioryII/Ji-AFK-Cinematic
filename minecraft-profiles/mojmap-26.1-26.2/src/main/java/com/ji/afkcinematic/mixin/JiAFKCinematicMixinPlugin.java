package com.ji.afkcinematic.mixin;

import com.ji.afkcinematic.diagnostic.MixinState;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

/**
 * Records transformed-bytecode injection evidence after application.
 * A merged handler from a {@code require = 0} injector can exist without an
 * INVOKE from vanilla; postApply alone never proves installation or execution.
 *
 * <p>Registered in {@code ji-afk-cinematic.mixins.json} via {@code "plugin": ...}.
 */
public class JiAFKCinematicMixinPlugin implements IMixinConfigPlugin {

    @Override
    public void onLoad(String mixinPackage) {
        // no-op
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        String version = com.ji.afkcinematic.PlatformAccess.minecraftVersion();
        boolean is262 = version.startsWith("26.2");
        if (mixinClassName.endsWith("LegacyOptionsHudAccess")) return !is262;
        if (mixinClassName.endsWith("DedicatedHudStateAccess")) return is262;
        if (mixinClassName.endsWith("Hud262Mixin")) return is262;
        if (mixinClassName.endsWith("InGameHudMixin")) return !is262;
        if (mixinClassName.endsWith("GuiPauseMixin")) return is262;
        if (mixinClassName.endsWith("MinecraftPauseMixin")) return !is262;
        return true;
    }

    @Override
    public void acceptTargets(java.util.Set<String> myTargets, java.util.Set<String> otherTargets) {
        // no-op: we don't contribute additional targets dynamically
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass,
                         String mixinClassName, IMixinInfo mixinInfo) {
        // no-op
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass,
                          String mixinClassName, IMixinInfo mixinInfo) {
        MixinState.inspectApplied(mixinClassName, targetClass);
    }

    @Override
    public java.util.List<String> getMixins() {
        return null; // no additional dynamic mixins
    }
}
