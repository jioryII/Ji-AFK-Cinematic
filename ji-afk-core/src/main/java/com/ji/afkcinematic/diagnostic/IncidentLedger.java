package com.ji.afkcinematic.diagnostic;

import java.util.ArrayList;
import java.util.List;

/** Client-thread ledger, bounded and deduplicated. IO and UI are separate owners. */
public final class IncidentLedger {
    public static final int CAPACITY=16;
    private final ArrayList<DiagnosticIncident> entries=new ArrayList<>();
    private long revision;
    public long revision(){return revision;}
    public List<DiagnosticIncident> snapshot(){return List.copyOf(entries);}
    public int pending(){int n=0;for(var e:entries)if(e.status()==DiagnosticIncident.Status.PENDING)n++;return n;}
    public void record(DiagnosticIncident.Code code,DiagnosticIncident.Confidence confidence,List<String> candidates,
                       long now,SemanticTrace.Snapshot trace) {
        record(code,confidence,candidates,now,trace,"development-unidentified");
    }
    public void record(DiagnosticIncident.Code code,DiagnosticIncident.Confidence confidence,List<String> candidates,
                       long now,SemanticTrace.Snapshot trace,String build) {
        for(int i=0;i<entries.size();i++) {
            var old=entries.get(i);
            if(old.code()!=code || old.confidence()!=confidence || !old.candidates().equals(candidates) || !old.build().equals(build))continue;
            entries.remove(i);
            entries.add(new DiagnosticIncident(code,confidence,candidates,old.firstSeen(),Math.max(old.lastSeen(),now),
                    old.occurrences()==Integer.MAX_VALUE?Integer.MAX_VALUE:old.occurrences()+1,DiagnosticIncident.Status.PENDING,
                    old.status()==DiagnosticIncident.Status.PENDING?old.trace():trace,build));
            revision++;return;
        }
        if(entries.size()==CAPACITY)entries.removeFirst();
        entries.add(new DiagnosticIncident(code,confidence,candidates,now,now,1,DiagnosticIncident.Status.PENDING,trace,build));revision++;
    }
    public void restore(List<DiagnosticIncident> saved) {
        for(int i=saved.size()-1;i>=0;i--) {
            var entry=saved.get(i);
            if(entries.stream().anyMatch(current->current.code()==entry.code() && current.confidence()==entry.confidence()
                    && current.candidates().equals(entry.candidates()) && current.build().equals(entry.build())))continue;
            // New runtime evidence wins over disk data arriving asynchronously.
            if(entries.size()==CAPACITY)break;
            entries.addFirst(entry);revision++;
        }
    }
    public void review(List<DiagnosticIncident> reviewed) {
        for(int i=0;i<entries.size();i++) {
            var old=entries.get(i);
            if(old.status()!=DiagnosticIncident.Status.PENDING || !reviewed.contains(old))continue;
            entries.set(i,new DiagnosticIncident(old.code(),old.confidence(),old.candidates(),old.firstSeen(),old.lastSeen(),
                    old.occurrences(),DiagnosticIncident.Status.REVIEWED,old.trace(),old.build()));revision++;
        }
    }
}
