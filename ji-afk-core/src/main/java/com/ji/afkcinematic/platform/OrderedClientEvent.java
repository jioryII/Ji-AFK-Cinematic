package com.ji.afkcinematic.platform;

import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/** An ordered callback port. Registrations during dispatch apply to the next dispatch. */
public final class OrderedClientEvent<T> {
    private final CopyOnWriteArrayList<Consumer<T>> callbacks = new CopyOnWriteArrayList<>();

    public void register(Consumer<T> callback) {
        callbacks.add(Objects.requireNonNull(callback));
    }

    public void fire(T context) {
        for (Consumer<T> callback : callbacks) callback.accept(context);
    }
}
