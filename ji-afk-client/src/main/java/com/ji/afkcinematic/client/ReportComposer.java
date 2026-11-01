package com.ji.afkcinematic.client;

import com.ji.afkcinematic.diagnostic.DiagnosticReport;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

/** Shared UI model. A suggestion never invokes the bug snapshot provider. */
public final class ReportComposer {
    public enum Type { BUG, SUGGESTION }
    private final Supplier<DiagnosticReport.Build> build;
    private final Supplier<DiagnosticReport.Snapshot> snapshot;
    private String message = "";
    private Type type = Type.BUG;
    private String name = "", email = "", language = "en", minecraftVersion = "";
    private DiagnosticReport.Loader loader;
    private boolean includeDiagnostic, updates;
    public void contact(String name, String email) { if(!Objects.equals(this.name,name)||!Objects.equals(this.email,email)){this.name=Objects.requireNonNull(name);this.email=Objects.requireNonNull(email);invalidate();} }
    public void updates(boolean value, String language) { if(updates!=value||!Objects.equals(this.language,language)){updates=value;this.language=language;invalidate();} }
    public boolean updates() { return updates; }
    public void includeDiagnostic(boolean value) { if(includeDiagnostic!=value){includeDiagnostic=value;invalidate();} }
    public boolean includeDiagnostic() { return includeDiagnostic; }
    public void environment(String mc, DiagnosticReport.Loader loader) { if(!Objects.equals(minecraftVersion,mc)||this.loader!=loader){minecraftVersion=mc;this.loader=loader;invalidate();} }

    private DiagnosticReport.Preview preview;
    private long revision;

    public ReportComposer(Supplier<DiagnosticReport.Build> build, Supplier<DiagnosticReport.Snapshot> snapshot) {
        this.build = Objects.requireNonNull(build); this.snapshot = Objects.requireNonNull(snapshot);
    }
    public void message(String value) {
        if (!Objects.equals(message, value)) { message = Objects.requireNonNull(value); invalidate(); }
    }
    public void type(Type value) {
        if (type != value) { type = Objects.requireNonNull(value); invalidate(); }
    }
    public String message() { return message; }
    public Type type() { return type; }
    public long revision() { return revision; }
    public void invalidate() { preview = null; ++revision; }
    public Optional<DiagnosticReport.Preview> preview() { return Optional.ofNullable(preview); }
    public DiagnosticReport.Preview prepare() {
        return prepareWithDiagnostic(type != Type.SUGGESTION && includeDiagnostic
                ? DiagnosticReport.bug(snapshot.get(),message,DiagnosticReport.MAX_BYTES-8192) : null);
    }
    /** The review keeps the same captured diagnostic while consent is toggled. */
    public DiagnosticReport.Preview prepareWithDiagnostic(DiagnosticReport.Preview diagnostic) {
        invalidate();
        boolean suggestion=type==Type.SUGGESTION;
        var actual=build.get();
        var environment=new DiagnosticReport.Build(actual.ji(),minecraftVersion.isEmpty()?actual.mc():minecraftVersion,
                loader==null?actual.loader():loader,actual.loaderVersion(),actual.api());
        preview=DiagnosticReport.feedback(suggestion,environment,!suggestion&&includeDiagnostic?diagnostic:null,message,name,email,updates,language);
        return preview;
    }
}
