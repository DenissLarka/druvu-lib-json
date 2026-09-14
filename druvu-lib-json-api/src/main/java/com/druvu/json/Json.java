package com.druvu.json;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

/**
 * The front door: build JSON fluently, parse JSON into a navigable {@link JsonValue}.
 *
 * <pre>{@code
 * String body = Json.object()
 *         .add("orderType", "MARKET")
 *         .add("quantity", new BigDecimal("2.5"))
 *         .addObject("instrument").add("isin", isin).end()
 *         .toJson();
 *
 * JsonObject token = Json.parse(response).asObject();
 * String accessToken = token.string("access_token");
 * long expiresIn = token.longValue("expires_in");
 * }</pre>
 *
 * <p>Every entry point exists in two forms: the plain one uses the backend discovered via {@code com.druvu.lib.loader}
 * (exactly one backend module on the class/module path), the other takes an explicit {@link JsonBackend}.
 */
public final class Json {

    @SuppressWarnings("rawtypes")
    private static final BackendHolder<JsonBackend> BACKEND =
            new BackendHolder<>(JsonBackend.class, "JSON", "com.druvu:druvu-lib-json-gson");

    private Json() {}

    /**
     * Lazily discovers the backend, so that explicit-backend usage works without any backend registered. A failed
     * discovery is not cached: it throws again on the next call, with the original cause attached.
     */
    static JsonBackend<?> defaultBackend() {
        return BACKEND.get();
    }

    /** @return a builder for a new JSON object, using the discovered backend. */
    public static JsonObjectBuilder<?> object() {
        return object(defaultBackend());
    }

    /**
     * @param backend the backend to build with.
     * @return a builder for a new JSON object.
     */
    public static JsonObjectBuilder<?> object(JsonBackend<?> backend) {
        return BuilderImpl.newObject(backend);
    }

    /** @return a builder for a new JSON array, using the discovered backend. */
    public static JsonArrayBuilder<?> array() {
        return array(defaultBackend());
    }

    /**
     * @param backend the backend to build with.
     * @return a builder for a new JSON array.
     */
    public static JsonArrayBuilder<?> array(JsonBackend<?> backend) {
        return BuilderImpl.newArray(backend);
    }

    /**
     * Build an array by mapping objects, using the discovered backend.
     *
     * @param transform the transformer for the objects.
     * @param objects the objects to map.
     * @param <T> the type of the objects.
     * @return a builder for the array of mapped objects.
     */
    public static <T> JsonArrayBuilder<?> array(Mapper<T> transform, Iterable<T> objects) {
        JsonArrayBuilder<?> array = array();
        for (T object : objects) {
            array.add(transform.map(object));
        }
        return array;
    }

    /**
     * Parse a complete JSON document, strictly: malformed input and trailing content are refused.
     *
     * @param json the document text.
     * @return the document's root value.
     * @throws JsonException if the input is not a single well-formed JSON document.
     */
    public static JsonValue parse(String json) {
        return parse(json, defaultBackend());
    }

    /**
     * Parse a complete JSON document with an explicit backend.
     *
     * @param json the document text.
     * @param backend the backend to parse with.
     * @return the document's root value.
     * @throws JsonException if the input is not a single well-formed JSON document.
     */
    public static JsonValue parse(String json, JsonBackend<?> backend) {
        return parse(new StringReader(json), backend);
    }

    /**
     * Parse a complete JSON document from a reader, strictly: malformed input and trailing content are refused.
     *
     * @param in the input to parse.
     * @return the document's root value.
     * @throws JsonException if reading fails or the input is not a single well-formed JSON document.
     */
    public static JsonValue parse(Reader in) {
        return parse(in, defaultBackend());
    }

    /**
     * Parse a complete JSON document from a reader with an explicit backend.
     *
     * @param in the input to parse.
     * @param backend the backend to parse with.
     * @return the document's root value.
     * @throws JsonException if reading fails or the input is not a single well-formed JSON document.
     */
    @SuppressWarnings("unchecked")
    public static JsonValue parse(Reader in, JsonBackend<?> backend) {
        try {
            Object node = ((JsonBackend<Object>) backend).parse(in);
            return BackedValue.wrap(backend, node, "$");
        } catch (JsonException e) {
            throw e;
        } catch (IOException | RuntimeException e) {
            throw new JsonException("JSON parsing failed: " + e.getMessage(), e);
        }
    }

    /**
     * Build a standalone primitive, e.g. for collecting into arrays.
     *
     * @param value the value, never {@code null} — JSON {@code null} is never built.
     * @return the new builder.
     */
    public static JsonBuilder value(String value) {
        return BuilderImpl.primitive(defaultBackend(), value, JsonBackend::of);
    }

    /**
     * Build a standalone primitive, e.g. for collecting into arrays.
     *
     * @param value the value, never {@code null} — JSON {@code null} is never built.
     * @return the new builder.
     */
    public static JsonBuilder value(Number value) {
        return BuilderImpl.primitive(defaultBackend(), value, JsonBackend::of);
    }

    /**
     * Build a standalone primitive, e.g. for collecting into arrays.
     *
     * @param value the value, never {@code null} — JSON {@code null} is never built.
     * @return the new builder.
     */
    public static JsonBuilder value(Boolean value) {
        return BuilderImpl.primitive(defaultBackend(), value, JsonBackend::of);
    }
}
