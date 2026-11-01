package com.ji.afkcinematic.cinematic;

import com.ji.afkcinematic.cinematic.director.CameraVector;
import com.ji.afkcinematic.cinematic.director.CompositionPath;
import com.ji.afkcinematic.cinematic.director.CompositionPreset;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;

/** Native subject/pose adapter; all composition geometry lives in the shared core. */
public class DirectedCameraShot extends AbstractCameraShot {
    private final CompositionPreset preset;
    private CompositionPath path;

    public DirectedCameraShot(CompositionPreset preset) {
        this.preset = preset;
        path = new CompositionPath(preset, CompositionPath.Variation.DEFAULT, 0);
    }
    public CompositionPreset preset() { return preset; }
    public void usePath(CompositionPath path) {
        if (path.preset() != preset) throw new IllegalArgumentException("Wrong composition preset");
        this.path = path;
    }
    @Override public void start() {
        path = new CompositionPath(preset, CompositionPath.Variation.DEFAULT,
                client.player == null ? 0 : client.player.getYRot());
    }
    public static CompositionPath.Subject subject(float tickDelta) {
        Minecraft client = Minecraft.getInstance();
        Vec3 position = client.player.getPosition(tickDelta);
        return new CompositionPath.Subject(vector(position), client.player.getEyeHeight(client.player.getPose()),
                client.player.getBbHeight(), client.options.fov().get(),
                client.getWindow().getWidth() / (double) Math.max(1, client.getWindow().getHeight()));
    }
    public CompositionPath.Pose pose(float seconds, float speed, float tickDelta) {
        return path.sample(subject(tickDelta), seconds, speed);
    }
    @Override public Vec3 updatePosition(float seconds, float speed, float tickDelta) {
        return isPlayerAvailable() ? nativeVector(pose(seconds, speed, tickDelta).position()) : Vec3.ZERO;
    }
    @Override public float updatePitch(float seconds, float speed, float tickDelta) {
        if (!isPlayerAvailable()) return 0;
        var pose = pose(seconds, speed, tickDelta);
        return pose.at(pose.position()).pitch();
    }
    @Override public float updateYaw(float seconds, float speed, float tickDelta) {
        if (!isPlayerAvailable()) return 0;
        var pose = pose(seconds, speed, tickDelta);
        return pose.at(pose.position()).yaw();
    }
    @Override public String getId() { return preset.id(); }
    public static CameraVector vector(Vec3 v) { return new CameraVector(v.x, v.y, v.z); }
    public static Vec3 nativeVector(CameraVector v) { return new Vec3(v.x(), v.y(), v.z()); }
}
