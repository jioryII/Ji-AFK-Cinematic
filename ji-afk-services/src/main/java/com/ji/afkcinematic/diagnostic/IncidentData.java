package com.ji.afkcinematic.diagnostic;

import java.util.*;

/** One bounded data representation for the local journal and diagnostic transport. */
final class IncidentData {
    private IncidentData() { }
    static List<Object> encode(List<DiagnosticIncident> incidents) {
        if(incidents.size()>IncidentLedger.CAPACITY)throw new IllegalArgumentException("Too many incidents");
        return incidents.stream().map(i->(Object)Map.of("code",i.code().name(),"confidence",i.confidence().name(),
                "candidates",i.candidates(),"firstSeen",i.firstSeen(),"lastSeen",i.lastSeen(),"count",i.occurrences(),
                "status",i.status().name(),"trace",i.trace().samples().stream().map(s->List.of(s.relativeTick(),s.event().name(),s.source().name(),s.detail().name())).toList(),
                "omittedTrace",i.trace().omitted(),"build",i.build())).toList();
    }
    static List<DiagnosticIncident> decode(Object value) {
        if(!(value instanceof List<?> rows) || rows.size()>IncidentLedger.CAPACITY)throw new IllegalArgumentException("Invalid incident journal");
        var result=new ArrayList<DiagnosticIncident>();
        for(var row:rows) {
            if(!(row instanceof Map<?,?> m) || !m.keySet().equals(Set.of("code","confidence","candidates","firstSeen","lastSeen","count","status","trace","omittedTrace","build")))throw new IllegalArgumentException("Invalid journal entry");
            var candidates=((List<?>)m.get("candidates")).stream().map(String.class::cast).toList();
            var samples=new ArrayList<SemanticTrace.Sample>();
            if(!(m.get("trace") instanceof List<?> events) || events.size()>SemanticTrace.CAPACITY)throw new IllegalArgumentException("Invalid trace");
            for(var event:events) {
                if(!(event instanceof List<?> e) || e.size()!=4)throw new IllegalArgumentException("Invalid trace event");
                samples.add(new SemanticTrace.Sample(Math.toIntExact(number(e.get(0))),SemanticTrace.Event.valueOf((String)e.get(1)),
                        SemanticTrace.Source.valueOf((String)e.get(2)),SemanticTrace.Detail.valueOf((String)e.get(3))));
            }
            result.add(new DiagnosticIncident(DiagnosticIncident.Code.valueOf((String)m.get("code")),
                    DiagnosticIncident.Confidence.valueOf((String)m.get("confidence")),candidates,number(m.get("firstSeen")),number(m.get("lastSeen")),
                    Math.toIntExact(number(m.get("count"))),DiagnosticIncident.Status.valueOf((String)m.get("status")),
                    new SemanticTrace.Snapshot(samples,Math.toIntExact(number(m.get("omittedTrace")))),(String)m.get("build")));
        }
        return List.copyOf(result);
    }
    private static long number(Object n) {
        if(n instanceof Integer i)return i;
        if(n instanceof java.math.BigDecimal d)return d.longValueExact();
        throw new IllegalArgumentException("Expected integer");
    }
}
