package com.ji.afkcinematic.diagnostic;

import java.util.LinkedHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.function.LongSupplier;

/** Per-kind admission and bounded receipts. Stores hashes only, never contact or report text. */
final class FeedbackGate {
    record Attempt(CompletableFuture<HttpReportTransport.Receipt> future, boolean owner) {}
    private record Pending(String fingerprint, CompletableFuture<HttpReportTransport.Receipt> future) {}
    private final LongSupplier clock;
    private final long[] until = new long[2];
    private final Pending[] pending = new Pending[2];
    private final LinkedHashMap<String,HttpReportTransport.Receipt> sent = new LinkedHashMap<>();
    FeedbackGate(LongSupplier clock) { this.clock=clock; }
    synchronized long seconds(boolean suggestion) { return Math.max(0,(until[suggestion?1:0]-clock.getAsLong()+999)/1000); }
    synchronized Attempt reserve(boolean suggestion,String fingerprint) {
        var receipt=sent.get(fingerprint);
        if(receipt!=null)return new Attempt(CompletableFuture.completedFuture(receipt),false);
        int kind=suggestion?1:0;var current=pending[kind];
        if(current!=null)return new Attempt(current.fingerprint.equals(fingerprint)?current.future:
                CompletableFuture.failedFuture(new HttpReportTransport.CooldownException(1)),false);
        long seconds=seconds(suggestion);
        if(seconds>0)return new Attempt(CompletableFuture.failedFuture(new HttpReportTransport.CooldownException(seconds)),false);
        var future=new CompletableFuture<HttpReportTransport.Receipt>();pending[kind]=new Pending(fingerprint,future);
        return new Attempt(future,true);
    }
    synchronized void defer(boolean suggestion,long seconds) {
        int kind=suggestion?1:0;until[kind]=Math.max(until[kind],clock.getAsLong()+Math.max(1,Math.min(3600,seconds))*1000);
    }
    void finish(boolean suggestion,String fingerprint,Attempt attempt,HttpReportTransport.Receipt receipt,Throwable error) {
        synchronized(this) {
            int kind=suggestion?1:0;var current=pending[kind];
            if(current!=null && current.future==attempt.future) {
                pending[kind]=null;
                if(error==null) {
                    until[kind]=clock.getAsLong()+180_000;
                    sent.put(fingerprint,receipt);
                    if(sent.size()>64)sent.remove(sent.keySet().iterator().next());
                }
            }
        }
        // Callbacks may acquire UI/transport locks; never invoke them while holding this lock.
        if(error==null)attempt.future.complete(receipt);else attempt.future.completeExceptionally(error);
    }
    synchronized void reset() { java.util.Arrays.fill(until,0);java.util.Arrays.fill(pending,null);sent.clear(); }
}
