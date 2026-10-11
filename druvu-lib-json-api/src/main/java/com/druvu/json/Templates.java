package com.druvu.json;

import java.time.temporal.Temporal;
import java.util.Objects;

/**
 * JSON text with {@code ?} placeholders outside its string literals, filled in order with values the backend
 * serializes. The splice is text, so a built or parsed fragment may come from any backend; the result is parsed
 * strictly afterwards by the caller, which is what makes a malformed template fail loudly.
 *
 * @author Deniss Larka <br>
 *     on 11 Oct 2026
 */
final class Templates {

    private Templates() {}

    static String fill(String template, JsonBackend<?> backend, Object[] values) {
        Objects.requireNonNull(template, "template");
        Objects.requireNonNull(values, "values");
        StringBuilder out = new StringBuilder(template.length() + 64);
        int next = 0;
        boolean inString = false;
        boolean escaped = false;
        for (int i = 0; i < template.length(); i++) {
            char c = template.charAt(i);
            if (inString) {
                out.append(c);
                if (escaped) {
                    escaped = false;
                } else if (c == '\\') {
                    escaped = true;
                } else if (c == '"') {
                    inString = false;
                }
            } else if (c == '"') {
                inString = true;
                out.append(c);
            } else if (c == '?') {
                if (next == values.length) {
                    throw new IllegalArgumentException(
                            "Placeholder " + (next + 1) + " has no value: " + values.length + " given");
                }
                out.append(text(values[next++], backend));
            } else {
                out.append(c);
            }
        }
        if (next < values.length) {
            throw new IllegalArgumentException(values.length + " values for " + next + " placeholders");
        }
        return out.toString();
    }

    /** One value as JSON text, through the backend, so a string is escaped the way the backend escapes it. */
    private static String text(Object value, JsonBackend<?> backend) {
        Objects.requireNonNull(value, "a placeholder value; JSON null is never built");
        return switch (value) {
            case JsonBuilder builder -> builder.toJson();
            case JsonValue parsed -> parsed.toJson();
            case String string ->
                BuilderImpl.primitive(backend, string, JsonBackend::of).toJson();
            case Number number ->
                BuilderImpl.primitive(backend, number, JsonBackend::of).toJson();
            case Boolean flag ->
                BuilderImpl.primitive(backend, flag, JsonBackend::of).toJson();
            case Temporal temporal ->
                BuilderImpl.primitive(backend, temporal, JsonBackend::of).toJson();
            default ->
                throw new IllegalArgumentException(
                        "A placeholder takes a String, Number, Boolean, Temporal, JsonValue or JsonBuilder, not "
                                + value.getClass().getName());
        };
    }
}
