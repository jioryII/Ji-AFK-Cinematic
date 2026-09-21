package com.ji.afkcinematic.mixin;

import com.ji.afkcinematic.diagnostic.MixinState;
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
        return true; // apply all declared mixins
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
