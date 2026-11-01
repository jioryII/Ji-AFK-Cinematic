package com.ji.afkcinematic.cinematic;
/** Bounded angular tracking without a slow-fast-slow easing curve. Reset on a deliberate cut. */
public final class CameraAim {
    private CameraFrame previous;
    public void reset(){previous=null;}
    public CameraFrame track(CameraFrame desired,double seconds){
        if(previous==null){previous=desired;return desired;}
        double limit=3*ShotMotion.PACE_ADJUSTMENT*Math.max(0,Math.min(.1,seconds));
        double yaw=(desired.yaw()-previous.yaw())%360;
        if(yaw>180)yaw-=360;if(yaw< -180)yaw+=360;
        float y=(float)(previous.yaw()+Math.max(-limit,Math.min(limit,yaw)));
        float p=(float)(previous.pitch()+Math.max(-limit,Math.min(limit,desired.pitch()-previous.pitch())));
        previous=new CameraFrame(desired.x(),desired.y(),desired.z(),p,y,desired.roll());return previous;
    }
}
