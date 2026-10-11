package com.druvu.json;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * The front door: build JSON fluently, fill a JSON template, parse JSON into a navigable {@link JsonValue}.
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
     * Build a new JSON object by writing its members in a body, using the discovered backend. The returned builder is
     * complete: serialize it, build it, or go on adding.
     *
     * @param body writes the object's members.
     * @return the builder holding the written object.
     */
    public static JsonObjectBuilder<?> object(Consumer<? super JsonObjectBody> body) {
        return object(defaultBackend(), body);
    }

    /**
     * Build a new JSON object by writing its members in a body, with an explicit backend.
     *
     * @param backend the backend to build with.
     * @param body writes the object's members.
     * @return the builder holding the written object.
     */
    public static JsonObjectBuilder<?> object(JsonBackend<?> backend, Consumer<? super JsonObjectBody> body) {
        JsonObjectBuilder<?> object = object(backend);
        Objects.requireNonNull(body, "body").accept(object);
        return object;
    }

    /**
     * Build a new JSON array by writing its elements in a body, using the discovered backend.
     *
     * @param body writes the array's elements.
     * @return the builder holding the written array.
     */
    public static JsonArrayBuilder<?> array(Consumer<? super JsonArrayBody> body) {
        return array(defaultBackend(), body);
    }

    /**
     * Build a new JSON array by writing its elements in a body, with an explicit backend.
     *
     * @param backend the backend to build with.
     * @param body writes the array's elements.
     * @return the builder holding the written array.
     */
    public static JsonArrayBuilder<?> array(JsonBackend<?> backend, Consumer<? super JsonArrayBody> body) {
        JsonArrayBuilder<?> array = array(backend);
        Objects.requireNonNull(body, "body").accept(array);
        return array;
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
     * Fill a JSON template: {@code ?} outside string literals is a placeholder, bound in order to the values given — a
     * {@link String}, {@link Number}, {@link Boolean}, {@link java.time.temporal.Temporal}, {@link JsonValue} or
     * {@link JsonBuilder}, each serialized by the backend, so a string is escaped and a built fragment splices in
     * whole. The filled text is then parsed strictly, as {@link #parse(String)} would. Use the builder when the shape
     * itself is computed; use a template when it is written down and only a few values move.
     *
     * <pre>{@code
     * JsonValue result = Json.fill("""
     *         {
     *           "protocolVersion": ?,
     *           "serverInfo": { "name": ?, "version": "1" }
     *         }
     *         """, version, name);
     * }</pre>
     *
     * @param template JSON text with {@code ?} placeholders.
     * @param values one value per placeholder, never {@code null} — JSON {@code null} is never built.
     * @return the filled document's root value.
     * @throws IllegalArgumentException if the placeholders and the values differ in number, or a value has a type a
     *     template cannot take.
     * @throws JsonException if the filled text is not a single well-formed JSON document.
     */
    public static JsonValue fill(String template, Object... values) {
        return fill(template, defaultBackend(), values);
    }

    /**
     * Fill a JSON template with an explicit backend; see {@link #fill(String, Object...)}.
     *
     * @param template JSON text with {@code ?} placeholders.
     * @param backend the backend that serializes the values and parses the result.
     * @param values one value per placeholder, never {@code null}.
     * @return the filled document's root value.
     */
    public static JsonValue fill(String template, JsonBackend<?> backend, Object... values) {
        return parse(Templates.fill(template, backend, values), backend);
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
