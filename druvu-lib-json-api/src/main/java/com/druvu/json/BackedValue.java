package com.druvu.json;

import java.io.IOException;
import java.io.Writer;

/**
 * Shared state and behaviour of the {@link JsonValue} implementations: a backend, the native node it wraps, and the
 * node's path within its document.
 *
 * <p>Equality is structural per backend: two values are equal when their native nodes are equal, so values from
 * different backends never compare equal.
 */
abstract class BackedValue {

    final JsonBackend<Object> backend;
    final Object node;
    private final String path;

    BackedValue(JsonBackend<Object> backend, Object node, String path) {
        this.backend = backend;
        this.node = node;
        this.path = path;
    }

    /** Wraps a native node in the {@link JsonValue} implementation matching its kind. */
    @SuppressWarnings("unchecked")
    static JsonValue wrap(JsonBackend<?> backend, Object node, String path) {
        JsonBackend<Object> b = (JsonBackend<Object>) backend;
        JsonValue.Kind kind = b.kindOf(node);
        return switch (kind) {
            case OBJECT -> new JsonObject(b, node, path);
            case ARRAY -> new JsonArray(b, node, path);
            default -> new JsonPrimitive(b, node, path, kind);
        };
    }

    public final String path() {
        return path;
    }

    public final Object raw() {
        return node;
    }

    public final String toJson() {
        return backend.serialize(node);
    }

    public final void write(Writer out) throws IOException {
        backend.write(node, out);
    }

    @Override
    public final String toString() {
        return toJson();
    }

    @Override
    public final boolean equals(Object obj) {
        return obj instanceof BackedValue other && node.equals(other.node);
    }

    @Override
    public final int hashCode() {
        return node.hashCode();
    }
}
