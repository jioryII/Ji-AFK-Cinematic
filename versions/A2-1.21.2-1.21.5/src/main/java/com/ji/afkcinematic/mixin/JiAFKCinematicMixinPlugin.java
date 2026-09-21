package com.ji.afkcinematic.mixin;

import com.ji.afkcinematic.diagnostic.MixinState;
import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

/**
 * Mixin config plugin for Ji-AFK-Cinematic. Its only job is to record, via
 * {@link #postApply}, which mixins successfully bound to their target class —
 * so the main entrypoint can warn at startup about any critical mixin whose
 * {@code require = 0} injector silently no-op'd against a future MC version.
 *
 * <p>Registered in {@code ji-afk-cinematic.mixins.json} via {@code "plugin": ...}.
 */
public class JiAFKCinematicMixinPlugin implements IMixinConfigPlugin {
    private static final String VANILLA_CULLING_MIXIN =
        "com.ji.afkcinematic.mixin.WorldRendererCullingMixin";

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
        // Sodium owns its renderer culling path; applying the vanilla culling
        // workaround at the same time can crash during renderer initialization.
        return !VANILLA_CULLING_MIXIN.equals(mixinClassName)
            || !FabricLoader.getInstance().isModLoaded("sodium");
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
        // Called only for mixins that successfully applied. Mixins whose target
        // class doesn't exist never reach this callback, so its absence signals a
        // silent bind failure.
        MixinState.markApplied(mixinClassName);
    }

    @Override
    public java.util.List<String> getMixins() {
        return null; // no additional dynamic mixins
    }
}
