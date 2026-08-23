package com.druvu.json;

import java.io.IOException;
import java.io.Writer;

/**
 * Common surface of all builders: finish into a {@link JsonValue}, or serialize directly. The builder hierarchy is
 * sealed — builders exist only through {@link Json}'s entry points.
 */
public sealed interface JsonBuilder permits JsonObjectBuilder, JsonArrayBuilder {

    /** @return the built JSON as a navigable value — the same type {@link Json#parse(String)} returns. */
    JsonValue build();

    /** @return the built JSON serialized as a string. */
    String toJson();

    /**
     * Write the JSON to a writer.
     *
     * @param out the output writer.
     * @throws IOException if writing fails.
     */
    void write(Writer out) throws IOException;

    /** @return the serialized JSON, same as {@link #toJson()}. */
    @Override
    String toString();
}
