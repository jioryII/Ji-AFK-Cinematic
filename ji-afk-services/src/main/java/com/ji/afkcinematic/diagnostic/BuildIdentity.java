package com.ji.afkcinematic.diagnostic;

import java.nio.charset.StandardCharsets;

/** Source fingerprint packaged by the assembler; not a signature or proof of report authenticity. */
public final class BuildIdentity {
    private static final String ID = read();
    private BuildIdentity() {}
    public static String id() { return ID; }
    private static String read() {
        try (var stream = BuildIdentity.class.getResourceAsStream("/ji-afk-build-id.txt")) {
            if (stream == null) return "development-unidentified";
            String text = new String(stream.readNBytes(81), StandardCharsets.US_ASCII).trim();
            return text.matches("sha256:[a-f0-9]{64}") ? text : "development-unidentified";
        } catch (java.io.IOException failure) { return "development-unidentified"; }
    }
}
