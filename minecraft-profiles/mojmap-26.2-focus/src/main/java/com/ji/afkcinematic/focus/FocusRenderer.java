package com.ji.afkcinematic.focus;

import com.ji.afkcinematic.JiAFKCinematic;
import com.ji.afkcinematic.cinematic.FocusStatus;
import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.*;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.systems.*;
import com.mojang.blaze3d.textures.FilterMode;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryStack;
import java.nio.ByteBuffer;
import java.util.Optional;

/** Native 26.2 GPU API only: works with either backend, never calls OpenGL/Vulkan directly. */
public final class FocusRenderer {
    private static final int UNIFORM_BYTES = 96;
    private static final BindGroupLayout GATHER_LAYOUT = BindGroupLayout.builder()
            .withSampler("SceneSampler").withSampler("DepthSampler")
            .withUniform("FocusConfig", UniformType.UNIFORM_BUFFER).build();
    private static final BindGroupLayout COMPOSITE_LAYOUT = BindGroupLayout.builder()
            .withSampler("SceneSampler").withSampler("DepthSampler").withSampler("BlurSampler")
            .withUniform("FocusConfig", UniformType.UNIFORM_BUFFER).build();
    private static TextureTarget scene, blur;
    private static RenderPipeline gather, composite;
    private static final Matrix4f inverseProjection = new Matrix4f();
    private static boolean projectionReady, depthReady, failed, announced;
    private static IrisDepthBridge.State irisState = IrisDepthBridge.State.ABSENT_OR_DISABLED;
    private static String status = "ready";
    private static String retainedDepthStatus;
    private static FocusScene.Snapshot frame = FocusScene.Snapshot.OFF;
    private FocusRenderer() {}

    public static void beginFrame() {
        frame = FocusScene.current(); projectionReady = depthReady = false;
        irisState = IrisDepthBridge.state();
        if (irisState == IrisDepthBridge.State.ABSENT_OR_DISABLED && "shader".equals(retainedDepthStatus))
            retainedDepthStatus = null;
        if (failed) setStatus("error");
        else if (irisState == IrisDepthBridge.State.UNKNOWN && frame.depthFocus()) {
            retainedDepthStatus = "shader"; setStatus("shader");
        } else if (!frame.optics().enabled() && retainedDepthStatus != null) setStatus(retainedDepthStatus);
        else setStatus("ready");
        if (!eligible()) releaseTargets();
    }

    /** Captures exactly the world projection, including bob/hurt; never the later HUD projection. */
    public static void projection(Matrix4f matrix) {
        if (!eligible()) return;
        inverseProjection.set(matrix).invert();
        projectionReady = inverseProjection.isFinite();
    }

    /** Vanilla clears world depth before the hand. Preserve it here, while it is still valid. */
    public static void captureDepth(RenderTarget main) {
        if (!eligible() || (frame.depthFocus() && !projectionReady)) return;
        try {
            if ((frame.depthFocus() && main.getDepthTexture() == null) || main.getColorTexture() == null
                    || main.width < 2 || main.height < 2) { retainedDepthStatus = "depth"; setStatus("depth"); return; }
            if (frame.depthFocus() && irisState == IrisDepthBridge.State.ACTIVE
                    && !IrisDepthBridge.usesMainWorldDepth(main)) { retainedDepthStatus = "shader"; setStatus("shader"); return; }
            ensureTargets(main);
            if (frame.depthFocus()) {
                if (main.getDepthTexture().getFormat() != scene.getDepthTexture().getFormat()) {
                    retainedDepthStatus = "depth"; setStatus("depth"); return;
                }
                scene.copyDepthFrom(main);
                retainedDepthStatus = null;
            }
            depthReady = true;
        } catch (RuntimeException error) { fail(error); }
    }

    /** Runs after world post effects and before GUI. Source and destination never alias. */
    public static void render(RenderTarget main) {
        if (!eligible() || !depthReady || (frame.depthFocus() && !projectionReady) || scene == null) return;
        try {
            if (main.width != scene.width || main.height != scene.height) return;
            var device = RenderSystem.getDevice();
            var encoder = device.createCommandEncoder();
            encoder.copyTextureToTexture(main.getColorTexture(), scene.getColorTexture(), 0, 0, 0, 0, 0, main.width, main.height);
            var optics = frame.optics();
            try (MemoryStack stack = MemoryStack.stackPush()) {
                ByteBuffer data = stack.malloc(UNIFORM_BYTES);
                inverseProjection.get(0, data);
                data.position(64);
                data.putFloat((float)optics.distance()).putFloat((float)optics.sharpHalfWidth())
                        .putFloat((float)Math.min(32, optics.radius1080() * main.height / 1080.0)).putFloat((float)optics.opacity());
                data.putFloat(1f / main.width).putFloat(1f / main.height)
                        .putFloat(device.getDeviceInfo().isZZeroToOne() ? 1 : 0).putFloat(frame.depthFocus() ? 0 : 1).flip();
                var uniforms = encoder.transientMemory().uploadGpu(data,
                        device.getDeviceInfo().limits().minUniformOffsetAlignment(), GpuBuffer.USAGE_UNIFORM);
                draw(encoder, gather, blur, uniforms, false);
                draw(encoder, composite, main, uniforms, true);
            }
            if (!announced) {
                announced = true;
                JiAFKCinematic.LOGGER.info("Optical focus 26.2 active: {}, {}x{}, 32 taps at half resolution",
                        RenderSystem.getBackendDescription(), main.width, main.height);
            }
        } catch (RuntimeException error) { fail(error); }
    }

    private static void draw(CommandEncoder encoder, RenderPipeline pipeline, RenderTarget output,
                             com.mojang.blaze3d.buffers.GpuBufferSlice uniforms, boolean compositing) {
        try (var pass = encoder.createRenderPass(() -> compositing ? "JI focus composite" : "JI focus gather",
                output.getColorTextureView(), Optional.empty())) {
            pass.setPipeline(pipeline);
            pass.setUniform("FocusConfig", uniforms);
            pass.bindTexture("SceneSampler", scene.getColorTextureView(), RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));
            pass.bindTexture("DepthSampler", scene.getDepthTextureView(), RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST));
            if (compositing) pass.bindTexture("BlurSampler", blur.getColorTextureView(),
                    RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));
            pass.draw(3, 1, 0, 0);
        }
    }

    private static void ensureTargets(RenderTarget main) {
        GpuFormat format = main.getColorTexture().getFormat();
        if (scene != null && (scene.width != main.width || scene.height != main.height || scene.getColorTexture().getFormat() != format))
            releaseTargets();
        if (scene == null) {
            scene = new TextureTarget("JI focus scene", main.width, main.height, true, format);
            blur = new TextureTarget("JI focus blur", Math.max(1, (main.width + 1)/2), Math.max(1, (main.height + 1)/2), false, GpuFormat.RGBA16_FLOAT);
            gather = pipeline("gather", GpuFormat.RGBA16_FLOAT);
            composite = pipeline("composite", format);
            var device = RenderSystem.getDevice();
            if (!device.precompilePipeline(gather).isValid() || !device.precompilePipeline(composite).isValid())
                throw new IllegalStateException("Optical focus shader compilation failed");
        }
    }

    private static RenderPipeline pipeline(String shader, GpuFormat format) {
        return RenderPipeline.builder().withLocation(Identifier.fromNamespaceAndPath("ji-afk-cinematic", "pipeline/focus_" + shader))
                .withVertexShader("core/screenquad")
                .withFragmentShader(Identifier.fromNamespaceAndPath("ji-afk-cinematic", "core/focus_" + shader))
                .withBindGroupLayout(shader.equals("gather") ? GATHER_LAYOUT : COMPOSITE_LAYOUT)
                .withCull(false).withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
                .withColorTargetState(new ColorTargetState(Optional.empty(), format, ColorTargetState.WRITE_ALL)).build();
    }

    private static boolean eligible() { return frame.optics().enabled() && !failed && status.equals("ready"); }
    private static void setStatus(String value) { status = value; FocusStatus.set(value); }
    private static void fail(RuntimeException error) {
        failed = true; setStatus("error"); releaseTargets();
        JiAFKCinematic.LOGGER.warn("Optical focus suspended until resource reload; the cinematic remains available", error);
    }
    private static void releaseTargets() {
        if (blur != null) { blur.destroyBuffers(); blur = null; }
        if (scene != null) { scene.destroyBuffers(); scene = null; }
        gather = composite = null;
    }
    public static void close() { releaseTargets(); depthReady = projectionReady = false; }
    public static void resourcesReloaded() { close(); failed = false; announced = false; retainedDepthStatus = null; setStatus("ready"); }
}
