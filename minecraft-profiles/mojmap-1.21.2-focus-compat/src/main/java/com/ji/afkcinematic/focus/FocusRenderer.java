package com.ji.afkcinematic.focus;

import com.ji.afkcinematic.JiAFKCinematic;
import com.ji.afkcinematic.cinematic.FocusStatus;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.framegraph.FrameGraphBuilder;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import com.mojang.blaze3d.resource.ResourceHandle;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.PostPass;
import net.minecraft.client.renderer.ShaderDefines;
import net.minecraft.client.renderer.ShaderProgram;
import net.minecraft.resources.ResourceLocation;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

/** 1.21.2 frame-graph passes, preserving world depth before vanilla clears it for the hand. */
public final class FocusRenderer {
    private static RenderTarget scene, blur;
    private static final ResourceLocation SCENE = ResourceLocation.fromNamespaceAndPath("ji-afk-cinematic", "scene");
    private static final ResourceLocation BLUR = ResourceLocation.fromNamespaceAndPath("ji-afk-cinematic", "blur");
    private static final ResourceLocation MAIN = ResourceLocation.fromNamespaceAndPath("ji-afk-cinematic", "main");
    private static final ShaderProgram GATHER = new ShaderProgram(ResourceLocation.withDefaultNamespace("post/ji_afk_focus_gather"), DefaultVertexFormat.POSITION, ShaderDefines.EMPTY);
    private static final ShaderProgram COMPOSITE = new ShaderProgram(ResourceLocation.withDefaultNamespace("post/ji_afk_focus_composite"), DefaultVertexFormat.POSITION, ShaderDefines.EMPTY);
    private static final Matrix4f inverseProjection = new Matrix4f();
    private static FocusScene.Snapshot frame = FocusScene.Snapshot.OFF;
    private static boolean projectionReady, depthReady, failed, announced;
    private static String retainedStatus;
    private FocusRenderer() {}

    public static void beginFrame() {
        frame = FocusScene.current();
        projectionReady = depthReady = false;
        if (failed) FocusStatus.set("error");
        else if (!frame.optics().enabled() && retainedStatus != null) FocusStatus.set(retainedStatus);
        else FocusStatus.set("ready");
        if (!eligible()) release();
    }

    public static void projection(Matrix4f projection) {
        if (!eligible()) return;
        inverseProjection.set(projection).invert();
        projectionReady = inverseProjection.isFinite();
    }

    public static void captureDepth(RenderTarget main) {
        if (!eligible() || (frame.depthFocus() && !projectionReady)) return;
        try {
            if (main.width < 2 || main.height < 2 || main.getColorTextureId() <= 0
                    || (frame.depthFocus() && main.getDepthTextureId() <= 0)) {
                retainedStatus = "depth"; FocusStatus.set("depth"); return;
            }
            if (frame.depthFocus() && !IrisDepthBridge.mainDepthIsSafe(main)) {
                retainedStatus = "shader"; FocusStatus.set("shader"); return;
            }
            ensure(main);
            if (frame.depthFocus()) scene.copyDepthFrom(main);
            main.bindWrite(true);
            retainedStatus = null;
            depthReady = true;
        } catch (Exception | LinkageError error) { fail(error); }
    }

    public static void render(RenderTarget main) {
        if (!eligible() || !depthReady || scene == null || main.width != scene.width || main.height != scene.height) return;
        try {
            // Preserve the post-processed scene; sampling the destination framebuffer is undefined.
            GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, main.frameBufferId);
            GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, scene.frameBufferId);
            GlStateManager._glBlitFrameBuffer(0, 0, main.width, main.height, 0, 0, main.width, main.height,
                    GL11.GL_COLOR_BUFFER_BIT, GL11.GL_NEAREST);
            main.bindWrite(true);
            var optics = frame.optics();
            float radius = (float)Math.min(32, optics.radius1080() * main.height / 1080.0);
            runPass(GATHER, BLUR, main, radius, false);
            runPass(COMPOSITE, MAIN, main, radius, true);
            main.bindWrite(true);
            if (!announced) {
                announced = true;
                JiAFKCinematic.LOGGER.info("Optical focus 1.21.2 active: {}x{}, 32 taps at half resolution", main.width, main.height);
            }
        } catch (Exception | LinkageError error) { main.bindWrite(true); fail(error); }
    }

    private static void runPass(ShaderProgram descriptor, ResourceLocation output, RenderTarget main, float radius, boolean withBlur) throws Exception {
        var shader = Minecraft.getInstance().getShaderManager().getProgramForLoading(descriptor);
        if (shader == null) throw new IllegalStateException("Focus shader unavailable: " + descriptor);
        var optics = frame.optics();
        shader.safeGetUniform("InverseProjection").set(inverseProjection);
        shader.safeGetUniform("Focus").set((float)optics.distance(), (float)optics.sharpHalfWidth(), radius, (float)optics.opacity());
        shader.safeGetUniform("Screen").set(1f / main.width, 1f / main.height, 0f, frame.depthFocus() ? 0f : 1f);
        var graph = new FrameGraphBuilder();
        Map<ResourceLocation, ResourceHandle<RenderTarget>> targets = new HashMap<>();
        targets.put(SCENE, graph.importExternal("focus-scene", scene));
        targets.put(BLUR, graph.importExternal("focus-blur", blur));
        targets.put(MAIN, graph.importExternal("focus-main", main));
        var pass = new PostPass("ji-afk-focus", shader, output, List.of());
        pass.addInput(new PostPass.TargetInput("Diffuse", SCENE, false, false));
        pass.addInput(new PostPass.TargetInput("Depth", SCENE, true, false));
        if (withBlur) pass.addInput(new PostPass.TargetInput("Blur", BLUR, false, true));
        RenderTarget destination = withBlur ? main : blur;
        pass.addToFrame(graph, targets, new Matrix4f().setOrtho(0, destination.width, 0, destination.height, .1f, 1000f));
        graph.execute(GraphicsResourceAllocator.UNPOOLED);
    }

    private static void ensure(RenderTarget main) throws Exception {
        if (scene != null && (scene.width != main.width || scene.height != main.height)) release();
        if (scene != null) return;
        scene = new TextureTarget(main.width, main.height, true);
        blur = new TextureTarget(Math.max(1,(main.width+1)/2), Math.max(1,(main.height+1)/2), false);
        blur.setFilterMode(GL11.GL_LINEAR);
    }

    private static boolean eligible() { return !failed && frame.optics().enabled() && FocusStatus.reason().equals("ready"); }
    private static void fail(Throwable error) {
        failed = true; FocusStatus.set("error"); release();
        JiAFKCinematic.LOGGER.warn("Optical focus suspended until resource reload; the cinematic remains available", error);
    }
    private static void release() {
        if (blur != null) { blur.destroyBuffers(); blur = null; }
        if (scene != null) { scene.destroyBuffers(); scene = null; }
    }
    public static void close() { release(); depthReady = projectionReady = false; }
    public static void resourcesReloaded() { close(); failed = false; announced = false; retainedStatus = null; FocusStatus.set("ready"); }
}
