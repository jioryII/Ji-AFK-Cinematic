package com.ji.afkcinematic.focus;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.renderpearl.api.textures.GpuTexture;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/** Optional, read-only bridge. Iris retains ownership of its buffers and settings. */
final class IrisDepthBridge {
    enum State { ABSENT_OR_DISABLED, ACTIVE, UNKNOWN }
    private static boolean resolved, unknown;
    private static Method apiInstance, packInUse, pipelineManager, pipelineNullable;
    private static Class<?> pipelineClass;
    private static Field renderTargets;
    private static Method depthTexture;
    private IrisDepthBridge() {}

    static State state() {
        if (!resolved) resolve();
        if (unknown) return State.UNKNOWN;
        if (apiInstance == null) return State.ABSENT_OR_DISABLED;
        try { return Boolean.TRUE.equals(packInUse.invoke(apiInstance.invoke(null))) ? State.ACTIVE : State.ABSENT_OR_DISABLED; }
        catch (ReflectiveOperationException | RuntimeException | LinkageError e) { return State.UNKNOWN; }
    }

    private static void resolve() {
        resolved = true;
        try {
            Class<?> api = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
            apiInstance = api.getMethod("getInstance"); packInUse = api.getMethod("isShaderPackInUse");
        } catch (ClassNotFoundException absent) { return; }
        catch (ReflectiveOperationException | RuntimeException | LinkageError error) { unknown = true; return; }
        try {
            Class<?> iris = Class.forName("net.irisshaders.iris.Iris");
            pipelineManager = iris.getMethod("getPipelineManager");
            pipelineNullable = pipelineManager.getReturnType().getMethod("getPipelineNullable");
            pipelineClass = Class.forName("net.irisshaders.iris.pipeline.IrisRenderingPipeline");
            renderTargets = pipelineClass.getDeclaredField("renderTargets");
            if (!renderTargets.trySetAccessible()) throw new IllegalAccessException("Iris targets unavailable");
            depthTexture = renderTargets.getType().getMethod("getDepthTexture");
        } catch (ReflectiveOperationException | RuntimeException | LinkageError error) {
            // Keep the public API check: disabled Iris must not affect vanilla.
            pipelineClass = null;
        }
    }

    /** Return true only when the active pipeline's depth is the main world texture. */
    static boolean usesMainWorldDepth(RenderTarget main) {
        if (state() != State.ACTIVE || pipelineClass == null) return false;
        try {
            Object manager = pipelineManager.invoke(null);
            Object pipeline = manager == null ? null : pipelineNullable.invoke(manager);
            if (!pipelineClass.isInstance(pipeline)) return false;
            Object targets = renderTargets.get(pipeline);
            Object texture = targets == null ? null : depthTexture.invoke(targets);
            return texture instanceof GpuTexture source && source == main.getDepthTexture()
                    && !source.isClosed() && source.getWidth(0) == main.width && source.getHeight(0) == main.height;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError error) { return false; }
    }
}
