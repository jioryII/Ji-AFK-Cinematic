package com.ji.afkcinematic.diagnostic;

import java.util.List;
import java.util.Objects;

/** Frozen evidence; no world coordinates, raw keys, free-form log text or personal data. */
public record DiagnosticIncident(Code code, Confidence confidence, List<String> candidates,
        long firstSeen, long lastSeen, int occurrences, Status status, SemanticTrace.Snapshot trace, String build) {
    public enum Code { CAMERA_NOT_APPLIED, CAMERA_OVERWRITTEN, CAMERA_CONSUMER_MISSING, HOOK_MISSING, ADAPTER_FAILURE, RESTORATION_FAILED }
    public enum Confidence { UNKNOWN, POSSIBLE, CONFIRMED }
    public enum Status { PENDING, REVIEWED, RESOLVED }
    public DiagnosticIncident(Code code,Confidence confidence,List<String> candidates,long firstSeen,long lastSeen,
            int occurrences,Status status,SemanticTrace.Snapshot trace) {
        this(code,confidence,candidates,firstSeen,lastSeen,occurrences,status,trace,"development-unidentified");
    }
    public DiagnosticIncident {
        Objects.requireNonNull(code);Objects.requireNonNull(confidence);Objects.requireNonNull(status);Objects.requireNonNull(trace);
        if(build==null || !build.matches("sha256:[a-f0-9]{64}|development-unidentified"))throw new IllegalArgumentException("Invalid build");
        candidates=List.copyOf(candidates);
        if(candidates.size()>3 || candidates.stream().anyMatch(id->!id.matches("[a-z0-9_.-]{1,128}"))
                || firstSeen<0 || lastSeen<firstSeen || lastSeen>9_007_199_254_740_991L || occurrences<1
                || trace.samples().size()>SemanticTrace.CAPACITY)throw new IllegalArgumentException("Invalid incident");
        if(confidence==Confidence.CONFIRMED && candidates.size()!=1)throw new IllegalArgumentException("Confirmed attribution needs one provider");
    }
}
