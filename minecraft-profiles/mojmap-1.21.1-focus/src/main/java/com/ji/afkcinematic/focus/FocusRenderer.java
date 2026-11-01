package com.ji.afkcinematic.focus;

import com.ji.afkcinematic.JiAFKCinematic;
import com.ji.afkcinematic.cinematic.FocusStatus;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.PostPass;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

/** Native 1.21.1 post passes, preserving the world's depth before vanilla clears it for the hand. */
public final class FocusRenderer {
    private static RenderTarget scene, blur;
    private static PostPass gather, composite;
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
            for (PostPass pass : new PostPass[]{gather, composite}) {
                var effect = pass.getEffect();
                effect.safeGetUniform("InverseProjection").set(inverseProjection);
                effect.safeGetUniform("Focus").set((float)optics.distance(), (float)optics.sharpHalfWidth(), radius, (float)optics.opacity());
                effect.safeGetUniform("Screen").set(1f/main.width, 1f/main.height, 0f, frame.depthFocus() ? 0f : 1f);
                pass.process(0f);
            }
            main.bindWrite(true);
            if (!announced) {
                announced = true;
                JiAFKCinematic.LOGGER.info("Optical focus 1.21.1 active: {}x{}, 32 taps at half resolution", main.width, main.height);
            }
        } catch (Exception | LinkageError error) { main.bindWrite(true); fail(error); }
    }

    private static void ensure(RenderTarget main) throws Exception {
        if (scene != null && (scene.width != main.width || scene.height != main.height)) release();
        if (scene != null) return;
        scene = new TextureTarget(main.width, main.height, true, Minecraft.ON_OSX);
        blur = new TextureTarget(Math.max(1,(main.width+1)/2), Math.max(1,(main.height+1)/2), false, Minecraft.ON_OSX);
        blur.setFilterMode(GL11.GL_LINEAR);
        var resources = Minecraft.getInstance().getResourceManager();
        gather = new PostPass(resources, "ji_afk_focus_gather", scene, blur, true);
        composite = new PostPass(resources, "ji_afk_focus_composite", scene, main, true);
        gather.addAuxAsset("DepthSampler", () -> scene.getDepthTextureId(), main.width, main.height);
        composite.addAuxAsset("DepthSampler", () -> scene.getDepthTextureId(), main.width, main.height);
        composite.addAuxAsset("BlurSampler", () -> blur.getColorTextureId(), blur.width, blur.height);
        gather.setOrthoMatrix(new Matrix4f().setOrtho(0, blur.width, 0, blur.height, .1f, 1000));
        composite.setOrthoMatrix(new Matrix4f().setOrtho(0, main.width, 0, main.height, .1f, 1000));
    }

    private static boolean eligible() { return !failed && frame.optics().enabled() && FocusStatus.reason().equals("ready"); }
    private static void fail(Throwable error) {
        failed = true; FocusStatus.set("error"); release();
        JiAFKCinematic.LOGGER.warn("Optical focus suspended until resource reload; the cinematic remains available", error);
    }
    private static void release() {
        if (gather != null) { gather.close(); gather = null; }
        if (composite != null) { composite.close(); composite = null; }
        if (blur != null) { blur.destroyBuffers(); blur = null; }
        if (scene != null) { scene.destroyBuffers(); scene = null; }
    }
    public static void close() { release(); depthReady = projectionReady = false; }
    public static void resourcesReloaded() { close(); failed = false; announced = false; retainedStatus = null; FocusStatus.set("ready"); }
}
