package com.ji.afkcinematic.cinematic;
import com.ji.afkcinematic.cinematic.director.CameraVector;
/** Fishing movement uses the same constant, calm rate independently of the native API. */
public final class FishingCameraMath {
    private FishingCameraMath() {}
    public static double travel(double seconds, double speed) { return Math.max(0,seconds) * Math.max(0,Math.min(1,speed)) * ShotMotion.PACE_ADJUSTMENT / 18; }
    public static float pitch(CameraVector camera,CameraVector target) {
        CameraVector d=target.subtract(camera);return (float)-Math.toDegrees(Math.atan2(d.y(),Math.hypot(d.x(),d.z())));
    }
    public static float yaw(CameraVector camera,CameraVector target) {
        CameraVector d=target.subtract(camera);return (float)Math.toDegrees(Math.atan2(-d.x(),d.z()));
    }
}
