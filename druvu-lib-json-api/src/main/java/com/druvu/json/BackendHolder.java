package com.druvu.json;

import com.druvu.lib.loader.ComponentLoader;
import com.druvu.lib.loader.TargetClassNotFoundException;

/**
 * Lazily discovers the one backend registered for a front door, and remembers it.
 *
 * <p>Discovery is deferred so that explicit-backend usage works with no backend registered at all, and a failure is
 * never cached: the next call tries again and throws again, with the original cause attached.
 *
 * @param <B> the backend type discovery matches on — the front door's identity, since {@code com.druvu.lib.loader}
 *     matches a factory by the exact class it declares.
 */
final class BackendHolder<B> {

    private final Class<B> type;
    private final String format;
    private final String module;

    private volatile B backend;

    BackendHolder(Class<B> type, String format, String module) {
        this.type = type;
        this.format = format;
        this.module = module;
    }

    B get() {
        B discovered = backend;
        if (discovered == null) {
            synchronized (this) {
                discovered = backend;
                if (discovered == null) {
                    discovered = discover();
                    backend = discovered;
                }
            }
        }
        return discovered;
    }

    private B discover() {
        try {
            return ComponentLoader.load(type);
        } catch (TargetClassNotFoundException e) {
            throw new IllegalStateException(
                    "No " + format + " backend found. Add a backend module (e.g. " + module
                            + ") to the class path or module path, or pass a " + type.getSimpleName() + " explicitly.",
                    e);
        }
    }
}
