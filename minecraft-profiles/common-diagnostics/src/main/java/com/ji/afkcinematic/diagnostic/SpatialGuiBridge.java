package com.ji.afkcinematic.diagnostic;

import java.lang.reflect.Method;

/** Optional, read-only API bridge. Method lookup happens once; state is refreshed by context generation. */
public final class SpatialGuiBridge {
    private Method active,closing,enabled,renderer,hooked;
    private boolean present,usable,busy;
    private long generation=Long.MIN_VALUE;
    private Object screen;
    public SpatialGuiBridge() {
        try {
            Class<?> api=Class.forName("org.tastytrash.spatialGUI.api.SpatialGUIAPI",false,getClass().getClassLoader());
            present=true;active=api.getMethod("isActive");closing=api.getMethod("isClosingScreen");
            enabled=Class.forName("org.tastytrash.spatialGUI.client.SpatialGUIClient",false,getClass().getClassLoader()).getMethod("isEnabled");usable=true;
        } catch(ReflectiveOperationException ignored) {
            try {
                Class<?> client=Class.forName("org.tastytrash.spatialGUI.client.SpatialGUIClient",false,getClass().getClassLoader());
                present=true;enabled=client.getMethod("isEnabled");renderer=client.getMethod("renderer");
                hooked=renderer.getReturnType().getMethod("getHookedScreen");
                try{closing=renderer.getReturnType().getMethod("isClosing");}catch(NoSuchMethodException oldVersion){closing=null;}
                usable=true;
            } catch(ReflectiveOperationException | RuntimeException | LinkageError unavailable){usable=false;}
        } catch(LinkageError unavailable){present=true;usable=false;}
        catch(RuntimeException unavailable){present=true;usable=false;}
    }
    public boolean present(){return present;}
    public boolean usable(){return usable;}
    public boolean busy(long generation,Object screen) {
        if(!usable)return false;
        if(this.generation==generation && this.screen==screen)return busy;
        this.generation=generation;this.screen=screen;
        try {
            if(active!=null)busy=Boolean.TRUE.equals(enabled.invoke(null))
                    && (Boolean.TRUE.equals(active.invoke(null)) || Boolean.TRUE.equals(closing.invoke(null)));
            else {
                Object current=renderer.invoke(null);
                busy=Boolean.TRUE.equals(enabled.invoke(null)) && current!=null
                        && (hooked.invoke(current)!=null || closing!=null && Boolean.TRUE.equals(closing.invoke(current)));
            }
        } catch(ReflectiveOperationException | RuntimeException | LinkageError changed){usable=false;busy=false;}
        return busy;
    }
}
