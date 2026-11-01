package com.ji.afkcinematic.client;
/** Shared lifecycle for asynchronous UI work; a detached generation cannot own a result. */
public final class UiWorkScope {
    private long generation;
    public void begin(){generation++;}
    public long generation(){return generation;}
    public boolean accepts(long token){return token==generation;}
}
