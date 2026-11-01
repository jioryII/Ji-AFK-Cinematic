package com.ji.afkcinematic.diagnostic;

import com.ji.afkcinematic.config.AtomicConfigFile;
import com.ji.afkcinematic.files.ManagedJobs;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/** Serialized worker, bounded input, atomic replacement. No network or raw game log access. */
public final class IncidentRepository {
    private static final int MAX_BYTES=131072;
    private final Path path;
    private final ManagedJobs jobs=new ManagedJobs("Ji-AFK-Diagnostics");
    public IncidentRepository(Path path){this.path=path;}
    public CompletableFuture<List<DiagnosticIncident>> read() {
        return jobs.submit(()->{
            if(!Files.exists(path))return List.of();
            try(var stream=Files.newInputStream(path)) {
                byte[] bytes=stream.readNBytes(MAX_BYTES+1);
                if(bytes.length>MAX_BYTES)throw new IllegalArgumentException("Incident journal too large");
                Object data=JsonData.parse(new String(bytes,StandardCharsets.UTF_8),8);
                if(!(data instanceof Map<?,?> m) || !m.keySet().equals(Set.of("schema","incidents")) || !Integer.valueOf(1).equals(m.get("schema")))
                    throw new IllegalArgumentException("Unknown incident journal");
                return IncidentData.decode(m.get("incidents"));
            } catch(java.io.IOException e){throw new java.io.UncheckedIOException(e);}
        });
    }
    public CompletableFuture<Void> write(List<DiagnosticIncident> snapshot) {
        var frozen=List.copyOf(snapshot);
        return jobs.submit(()->{
            String data=DiagnosticReport.json(Map.of("schema",1,"incidents",IncidentData.encode(frozen)));
            if(data.getBytes(StandardCharsets.UTF_8).length>MAX_BYTES)throw new IllegalArgumentException("Incident journal too large");
            try{new AtomicConfigFile(path).write(data);}catch(java.io.IOException e){throw new java.io.UncheckedIOException(e);}
            return null;
        });
    }
    public boolean shutdown(Duration timeout){return jobs.drain(timeout);}
}
