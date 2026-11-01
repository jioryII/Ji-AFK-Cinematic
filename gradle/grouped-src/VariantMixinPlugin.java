package com.ji.afkcinematic.group;

import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

/** Contributes mixin names only for the selected variant, before target discovery. */
public final class VariantMixinPlugin implements IMixinConfigPlugin {
    private IMixinConfigPlugin delegate;
    private List<String> mixins = List.of();
    private boolean active;
    private String mixinPackage;

    @Override public void onLoad(String mixinPackage) {
        this.mixinPackage = mixinPackage;
        if (!GroupSelection.isClient()) return;
        Properties config = GroupSelection.properties("META-INF/ji-afk-variants/" + mixinPackage + ".properties");
        active = GroupSelection.version().equals(config.getProperty("minecraft"));
        if (!active) return;
        String names = config.getProperty("mixins", "");
        mixins = names.isEmpty() ? List.of() : Arrays.asList(names.split(","));
        String plugin = config.getProperty("delegate");
        if (plugin != null && !plugin.isEmpty()) {
            try {
                delegate = (IMixinConfigPlugin) Class.forName(plugin).getConstructor().newInstance();
                delegate.onLoad(mixinPackage);
            } catch (ReflectiveOperationException error) {
                throw GroupSelection.failure("Cannot initialize mixin variant " + mixinPackage, error);
            }
        }
    }
    @Override public List<String> getMixins() {
        if (delegate == null) return mixins;
        java.util.ArrayList<String> all = new java.util.ArrayList<>(mixins);
        List<String> additional = delegate.getMixins();
        if (additional != null) all.addAll(additional);
        // Mixin treats plugin-contributed names as already selected and skips
        // shouldApplyMixin for them. These delegates select version alternatives
        // by mixin name (the target argument is unused), so filter here as well.
        all.removeIf(name -> !delegate.shouldApplyMixin("", mixinPackage + "." + name));
        return all;
    }
    @Override public String getRefMapperConfig() { return delegate == null ? null : delegate.getRefMapperConfig(); }
    @Override public boolean shouldApplyMixin(String target, String mixin) {
        return active && (delegate == null || delegate.shouldApplyMixin(target, mixin));
    }
    @Override public void acceptTargets(Set<String> mine, Set<String> others) {
        if (delegate != null) delegate.acceptTargets(mine, others);
    }
    @Override public void preApply(String target, ClassNode node, String mixin, IMixinInfo info) {
        if (delegate != null) delegate.preApply(target, node, mixin, info);
    }
    @Override public void postApply(String target, ClassNode node, String mixin, IMixinInfo info) {
        if (delegate != null) delegate.postApply(target, node, mixin, info);
    }
}
