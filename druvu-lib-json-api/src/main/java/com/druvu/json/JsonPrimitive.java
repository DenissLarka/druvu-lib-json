package com.druvu.json;

import java.math.BigDecimal;

/**
 * A JSON leaf value: a string, a number, a boolean, or JSON {@code null}. The typed accessors are strict — only the
 * accessor matching {@link #kind()} answers; the others throw {@link JsonException}.
 */
public final class JsonPrimitive extends BackedValue implements JsonValue {

    private final Kind kind;

    JsonPrimitive(JsonBackend<Object> backend, Object node, String path, Kind kind) {
        super(backend, node, path);
        this.kind = kind;
    }

    @Override
    public Kind kind() {
        return kind;
    }

    @Override
    public String asString() {
        if (kind != Kind.STRING) {
            return JsonValue.super.asString();
        }
        return backend.asString(node);
    }

    @Override
    public BigDecimal asDecimal() {
        if (kind != Kind.NUMBER) {
            return JsonValue.super.asDecimal();
        }
        return backend.asDecimal(node);
    }

    @Override
    public boolean asBoolean() {
        if (kind != Kind.BOOLEAN) {
            return JsonValue.super.asBoolean();
        }
        return backend.asBoolean(node);
    }
}
