package com.ji.afkcinematic.focus;

import com.mojang.blaze3d.pipeline.RenderTarget;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/** Verify ownership before using a shader pack's depth; never mutate the pack. */
final class IrisDepthBridge {
    private static boolean resolved, unknown;
    private static Method apiInstance, packInUse, pipelineManager, pipelineNullable, depthTexture;
    private static Class<?> pipelineClass;
    private static Field renderTargets;
    private IrisDepthBridge() {}

    private static void resolve() {
        resolved = true;
        Class<?> api = null;
        for (String name : new String[]{"net.irisshaders.iris.api.v0.IrisApi", "net.coderbot.iris.api.v0.IrisApi"}) {
            try { api = Class.forName(name); break; } catch (ClassNotFoundException absent) { /* optional */ }
            catch (LinkageError error) { unknown = true; return; }
        }
        if (api == null) return;
        try {
            apiInstance = api.getMethod("getInstance"); packInUse = api.getMethod("isShaderPackInUse");
            String prefix = api.getName().startsWith("net.coderbot") ? "net.coderbot.iris" : "net.irisshaders.iris";
            Class<?> iris = Class.forName(prefix + ".Iris");
            pipelineManager = iris.getMethod("getPipelineManager");
            pipelineNullable = pipelineManager.getReturnType().getMethod("getPipelineNullable");
            pipelineClass = Class.forName(prefix + ".pipeline.IrisRenderingPipeline");
            renderTargets = pipelineClass.getDeclaredField("renderTargets");
            if (!renderTargets.trySetAccessible()) throw new IllegalAccessException();
            depthTexture = renderTargets.getType().getMethod("getDepthTexture");
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            // Unknown internals must never be treated as validated depth.
            pipelineClass = null;
            if (apiInstance == null || packInUse == null) unknown = true;
        }
    }

    static boolean mainDepthIsSafe(RenderTarget main) {
        if (!resolved) resolve();
        if (unknown) return false;
        if (apiInstance == null) return true;
        try {
            if (!Boolean.TRUE.equals(packInUse.invoke(apiInstance.invoke(null)))) return true;
            if (pipelineClass == null) return false;
            Object manager = pipelineManager.invoke(null);
            Object pipeline = manager == null ? null : pipelineNullable.invoke(manager);
            if (!pipelineClass.isInstance(pipeline)) return false;
            Object targets = renderTargets.get(pipeline);
            Object depth = targets == null ? null : depthTexture.invoke(targets);
            return depth instanceof Number id && id.intValue() == main.getDepthTextureId();
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) { return false; }
    }
}
