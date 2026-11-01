package com.ji.afkcinematic.session;

/** Attempts every owned output even if a preceding adapter fails. No exception is silently lost. */
public final class RestorationSteps {
    private RestorationSteps() {}

    public static void run(Runnable... steps) {
        Throwable first = null;
        for (Runnable step : steps) {
            try { step.run(); }
            catch (RuntimeException | Error failure) {
                if (first == null) first = failure;
                else if (first != failure) first.addSuppressed(failure);
            }
        }
        if (first instanceof RuntimeException failure) throw failure;
        if (first instanceof Error failure) throw failure;
    }
}
