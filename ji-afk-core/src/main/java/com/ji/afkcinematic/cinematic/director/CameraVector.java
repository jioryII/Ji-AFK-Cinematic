package com.ji.afkcinematic.cinematic.director;

/** Geometry shared by camera planning and native adapters; no game dependencies. */
public record CameraVector(double x, double y, double z) {
    public static final CameraVector ZERO = new CameraVector(0, 0, 0);
    public CameraVector add(CameraVector v) { return new CameraVector(x + v.x, y + v.y, z + v.z); }
    public CameraVector subtract(CameraVector v) { return new CameraVector(x - v.x, y - v.y, z - v.z); }
    public CameraVector scale(double s) { return new CameraVector(x * s, y * s, z * s); }
    public double dot(CameraVector v) { return x * v.x + y * v.y + z * v.z; }
    public CameraVector cross(CameraVector v) { return new CameraVector(y * v.z - z * v.y, z * v.x - x * v.z, x * v.y - y * v.x); }
    public double length() { return Math.sqrt(dot(this)); }
    public double distance(CameraVector v) { return subtract(v).length(); }
    public boolean finite() { return Double.isFinite(x) && Double.isFinite(y) && Double.isFinite(z); }
    public CameraVector normalized() {
        double length = length();
        return length > 1.0e-9 && Double.isFinite(length) ? scale(1 / length) : ZERO;
    }
    public static CameraVector direction(double yaw, double pitch) {
        double y = Math.toRadians(yaw), p = Math.toRadians(pitch);
        return new CameraVector(-Math.sin(y) * Math.cos(p), -Math.sin(p), Math.cos(y) * Math.cos(p));
    }
}
