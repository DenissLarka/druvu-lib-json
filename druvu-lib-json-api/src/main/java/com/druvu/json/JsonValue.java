package com.druvu.json;

import java.io.IOException;
import java.io.Writer;
import java.math.BigDecimal;

/**
 * A JSON value — the result of {@link Json#parse(String) parsing} or {@link JsonBuilder#build() building}.
 *
 * <p>The hierarchy is sealed: a value is a {@link JsonObject}, a {@link JsonArray}, or a {@link JsonPrimitive} (string,
 * number, boolean or JSON null), so it can be deconstructed exhaustively:
 *
 * <pre>{@code
 * switch (value) {
 *     case JsonObject o -> ...
 *     case JsonArray a -> ...
 *     case JsonPrimitive p -> ...
 * }
 * }</pre>
 *
 * <p>All typed accessors are strict: asking a value for a type it does not hold throws {@link JsonException} naming the
 * expected kind, the actual kind and the {@linkplain #path() path} — nothing is coerced. Numbers are exposed as
 * {@link BigDecimal} (or via the exact integer accessors); there is deliberately no floating-point accessor.
 *
 * <p>{@code equals} carries no promise of its own: it delegates to the backend's native node, which compares
 * structurally on some engines and by identity on others. Compare what you read out of two documents rather than the
 * values themselves; values from different backends are never equal in any case.
 */
public sealed interface JsonValue permits JsonObject, JsonArray, JsonPrimitive {

    /** The six JSON value kinds. */
    enum Kind {
        OBJECT,
        ARRAY,
        STRING,
        NUMBER,
        BOOLEAN,
        NULL
    }

    /** @return the kind of this value. */
    Kind kind();

    /** @return {@code true} if this value is JSON {@code null}. */
    default boolean isNull() {
        return kind() == Kind.NULL;
    }

    /**
     * @return this value as an object.
     * @throws JsonException if this value is not an object.
     */
    default JsonObject asObject() {
        if (this instanceof JsonObject o) {
            return o;
        }
        throw typeMismatch(Kind.OBJECT);
    }

    /**
     * @return this value as an array.
     * @throws JsonException if this value is not an array.
     */
    default JsonArray asArray() {
        if (this instanceof JsonArray a) {
            return a;
        }
        throw typeMismatch(Kind.ARRAY);
    }

    /**
     * @return the string this value holds.
     * @throws JsonException if this value is not a string.
     */
    default String asString() {
        throw typeMismatch(Kind.STRING);
    }

    /**
     * @return the number this value holds, exactly as it appeared on the wire.
     * @throws JsonException if this value is not a number.
     */
    default BigDecimal asDecimal() {
        throw typeMismatch(Kind.NUMBER);
    }

    /**
     * @return the boolean this value holds.
     * @throws JsonException if this value is not a boolean.
     */
    default boolean asBoolean() {
        throw typeMismatch(Kind.BOOLEAN);
    }

    /**
     * @return the number this value holds, when it is exactly representable as an {@code int}.
     * @throws JsonException if this value is not a number, or the number has a fraction or overflows an {@code int}.
     */
    default int asInt() {
        BigDecimal decimal = asDecimal();
        try {
            return decimal.intValueExact();
        } catch (ArithmeticException e) {
            throw new JsonException("Number " + decimal + " at " + path() + " is not an exact int", e);
        }
    }

    /**
     * @return the number this value holds, when it is exactly representable as a {@code long}.
     * @throws JsonException if this value is not a number, or the number has a fraction or overflows a {@code long}.
     */
    default long asLong() {
        BigDecimal decimal = asDecimal();
        try {
            return decimal.longValueExact();
        } catch (ArithmeticException e) {
            throw new JsonException("Number " + decimal + " at " + path() + " is not an exact long", e);
        }
    }

    /**
     * @return where this value sits in its document, JSONPath-style (e.g. {@code $.items[2].amount}) — for diagnostics;
     *     every {@link JsonException} thrown from an accessor includes it.
     */
    String path();

    /**
     * @return the backend's native node (e.g. Gson's {@code JsonElement}) — the interop escape hatch for handing the
     *     tree to backend-specific code. Callers cast it; everything else in this API needs no casts.
     */
    Object raw();

    /** @return this value serialized as a JSON string. */
    String toJson();

    /**
     * Serialize this value to a writer.
     *
     * @param out the output writer.
     * @throws IOException if writing fails.
     */
    void write(Writer out) throws IOException;

    private JsonException typeMismatch(Kind expected) {
        return new JsonException("Expected " + expected + " but found " + kind() + " at " + path());
    }
}
