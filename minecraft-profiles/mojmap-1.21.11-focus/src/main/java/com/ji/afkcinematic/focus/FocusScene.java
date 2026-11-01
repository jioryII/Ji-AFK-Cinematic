package com.ji.afkcinematic.focus;

import com.ji.afkcinematic.cinematic.*;
import com.ji.afkcinematic.cinematic.director.CompositionPreset;
import com.ji.afkcinematic.config.ConfigManager;
import com.ji.afkcinematic.render.HUDController;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Camera;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/** World queries only during extraction; rendering consumes an immutable snapshot. */
public final class FocusScene {
    public record Snapshot(FocusController.Frame optics, long generation, boolean depthFocus) {
        static final Snapshot OFF = new Snapshot(FocusController.Frame.OFF, 0, false);
    }
    private static final FocusController OPTICS = new FocusController();
    private static volatile Snapshot current = Snapshot.OFF;
    private static Object world;
    private static long take = Long.MIN_VALUE, lastTime;
    private static final Vector3f transformed = new Vector3f();
    private FocusScene() {}
    public static Snapshot current() { return current; }

    public static void extract(Camera camera, float partialTick, boolean renderingWorld) {
        if (!FocusController.AVAILABLE) { clear(); return; }
        Minecraft mc = Minecraft.getInstance();
        var config = ConfigManager.getConfig();
        if (!renderingWorld || mc.level == null || mc.player == null || !config.modEnabled
                || config.focusLevel == 0 || !HUDController.isHidden()
                || CinematicManager.getState() != CinematicState.CINEMATIC_ACTIVE
                || !camera.isInitialized()) {
            clear(); return;
        }
        Matrix4f viewRotation = new Matrix4f().rotation(camera.rotation().conjugate(new Quaternionf()));
        String id = CameraController.getCurrentShotId();
        if (id.equals("none") || id.startsWith("fallback/")) { clear(); return; }
        long sequence = CameraController.getFocusTake();
        long now = System.nanoTime();
        boolean changed = world != mc.level || take != sequence;
        if (changed) { world = mc.level; take = sequence; }
        double dt = lastTime == 0 ? 1.0 / 60 : (now - lastTime) * 1e-9; lastTime = now;
        Vec3 target = CameraController.getOpticalFocusTarget(partialTick);
        double distance, band;
        if (target != null) {
            Vec3 delta = target.subtract(camera.position());
            transformed.set((float)delta.x, (float)delta.y, (float)delta.z);
            viewRotation.transformDirection(transformed);
            distance = -transformed.z;
            var selection = CameraController.getDirectorSelection();
            boolean portrait = selection != null && selection.path() != null
                    && selection.path().preset().motion == CompositionPreset.Motion.PORTRAIT;
            {
                // Project the current pose's volume onto the camera depth axis.
                double halfHeight = portrait ? Math.min(.32, mc.player.getBbHeight() * .3) : mc.player.getBbHeight() * .5;
                double halfWidth = mc.player.getBbWidth() * .55;
                var m = viewRotation;
                double extent = Math.abs(m.m02()) * halfWidth + Math.abs(m.m12()) * halfHeight + Math.abs(m.m22()) * halfWidth;
                Vec3 center = portrait ? target : mc.player.getPosition(partialTick).add(0, mc.player.getBbHeight() * .5, 0);
                Vec3 centerDelta = center.subtract(camera.position());
                transformed.set((float)centerDelta.x, (float)centerDelta.y, (float)centerDelta.z);
                m.transformDirection(transformed);
                band = extent + Math.abs(-transformed.z - distance) + .1;
            }
        } else { distance = 1; band = .1; }
        current = new Snapshot(OPTICS.update(mc.level, sequence, distance, band, config.focusLevel, dt), sequence, target != null);
    }

    private static void clear() {
        current = Snapshot.OFF; OPTICS.reset(); world = null; take = Long.MIN_VALUE;
        lastTime = 0;
    }
}
