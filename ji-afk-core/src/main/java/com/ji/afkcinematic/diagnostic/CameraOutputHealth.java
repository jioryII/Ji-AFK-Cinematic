package com.ji.afkcinematic.diagnostic;

/** Allocation-free comparison of one applied pose with its consumer, within the same render. */
public final class CameraOutputHealth {
    public enum Fault { NONE, NOT_APPLIED, OVERWRITTEN, CONSUMER_MISSING }
    private long epoch, render;
    private long appliedRender = -1;
    private double x,y,z,qx,qy,qz,qw;
    private int failures;
    private Fault previous = Fault.NONE;
    private boolean enabled;
    public void reset(long epoch) { this.epoch=epoch;render=0;appliedRender=-1;failures=0;previous=Fault.NONE;enabled=false; }
    public void begin(long epoch, boolean comparable) {
        if(this.epoch!=epoch)reset(epoch);
        render++;enabled=comparable;
        if(!comparable){failures=0;previous=Fault.NONE;}
    }
    public void applied(long epoch,double x,double y,double z,double qx,double qy,double qz,double qw) {
        if(!enabled || this.epoch!=epoch)return;
        appliedRender=render;this.x=x;this.y=y;this.z=z;this.qx=qx;this.qy=qy;this.qz=qz;this.qw=qw;
    }
    public Fault consume(long epoch,double x,double y,double z,double qx,double qy,double qz,double qw) {
        if(!enabled || this.epoch!=epoch)return Fault.NONE;
        enabled=false; // Duplicate consumers cannot increase the streak.
        Fault fault=appliedRender!=render?Fault.NOT_APPLIED:matches(x,y,z,qx,qy,qz,qw)?Fault.NONE:Fault.OVERWRITTEN;
        return streak(fault);
    }
    public Fault end() {
        if(!enabled)return Fault.NONE;
        enabled=false;return streak(Fault.CONSUMER_MISSING);
    }
    private Fault streak(Fault fault) {
        if(fault==Fault.NONE){failures=0;previous=fault;return fault;}
        failures=fault==previous?failures+1:1;previous=fault;
        return failures>=8?fault:Fault.NONE;
    }
    private boolean matches(double x,double y,double z,double qx,double qy,double qz,double qw) {
        double dx=this.x-x,dy=this.y-y,dz=this.z-z;
        double norm=(this.qx*this.qx+this.qy*this.qy+this.qz*this.qz+this.qw*this.qw)*(qx*qx+qy*qy+qz*qz+qw*qw);
        double dot=this.qx*qx+this.qy*qy+this.qz*qz+this.qw*qw;
        // 1 mm and approximately .05 degrees. q and -q describe the same orientation.
        return dx*dx+dy*dy+dz*dz<=1e-6 && norm>0 && Double.isFinite(norm) && dot*dot/norm>=.9999998;
    }
}
