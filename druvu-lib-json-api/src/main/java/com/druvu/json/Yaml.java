package com.druvu.json;

import java.io.Reader;

/**
 * The YAML front door: the same builders and the same {@link JsonValue} tree as {@link Json}, over a discovered
 * {@link YamlBackend}.
 *
 * <pre>{@code
 * String config = Yaml.object()
 *         .add("endpoint", url)
 *         .addArray("profiles").add("sandbox").add("prod").end()
 *         .toJson();
 *
 * JsonObject parsed = Yaml.parse(config).asObject();
 * }</pre>
 *
 * <p>A format is not a backend. Gson and Jackson are interchangeable ways to hold the same JSON, and asking for two of
 * them at once is an accident worth failing on; YAML is a different answer to "what should this text be", so it is
 * chosen at the call site instead of being discovered. Everything downstream is shared: the builders, the value types,
 * the strictness, and the {@linkplain JsonBackend backend SPI} itself.
 *
 * <p>The tree is the JSON data model throughout, so {@code toJson()} on a YAML-backed value is still the terminal that
 * serializes it — as YAML text.
 *
 * <p>Need both formats in one call, or none discovered? Every {@link Json} entry point also takes an explicit backend,
 * and a {@link YamlBackend} is a {@link JsonBackend}: {@code Json.object(new SnakeYamlBackend())}.
 */
public final class Yaml {

    @SuppressWarnings("rawtypes")
    private static final BackendHolder<YamlBackend> BACKEND =
            new BackendHolder<>(YamlBackend.class, "YAML", "com.druvu:druvu-lib-json-yaml");

    private Yaml() {}

    static YamlBackend<?> defaultBackend() {
        return BACKEND.get();
    }

    /** @return a builder for a new mapping, using the discovered backend. */
    public static JsonObjectBuilder<?> object() {
        return Json.object(defaultBackend());
    }

    /** @return a builder for a new sequence, using the discovered backend. */
    public static JsonArrayBuilder<?> array() {
        return Json.array(defaultBackend());
    }

    /**
     * Build a sequence by mapping objects, using the discovered backend.
     *
     * @param transform the transformer for the objects.
     * @param objects the objects to map.
     * @param <T> the type of the objects.
     * @return a builder for the sequence of mapped objects.
     */
    public static <T> JsonArrayBuilder<?> array(Mapper<T> transform, Iterable<T> objects) {
        JsonArrayBuilder<?> array = array();
        for (T object : objects) {
            array.add(transform.map(object));
        }
        return array;
    }

    /**
     * Parse a complete YAML document, strictly: malformed input and a second document are refused.
     *
     * @param yaml the document text.
     * @return the document's root value.
     * @throws JsonException if the input is not a single well-formed YAML document.
     */
    public static JsonValue parse(String yaml) {
        return Json.parse(yaml, defaultBackend());
    }

    /**
     * Parse a complete YAML document from a reader.
     *
     * @param in the input to parse.
     * @return the document's root value.
     * @throws JsonException if reading fails or the input is not a single well-formed YAML document.
     */
    public static JsonValue parse(Reader in) {
        return Json.parse(in, defaultBackend());
    }

    /**
     * Build a standalone scalar, e.g. for collecting into sequences.
     *
     * @param value the value, never {@code null} — a null is never built.
     * @return the new builder.
     */
    public static JsonBuilder value(String value) {
        return BuilderImpl.primitive(defaultBackend(), value, JsonBackend::of);
    }

    /**
     * Build a standalone scalar, e.g. for collecting into sequences.
     *
     * @param value the value, never {@code null} — a null is never built.
     * @return the new builder.
     */
    public static JsonBuilder value(Number value) {
        return BuilderImpl.primitive(defaultBackend(), value, JsonBackend::of);
    }

    /**
     * Build a standalone scalar, e.g. for collecting into sequences.
     *
     * @param value the value, never {@code null} — a null is never built.
     * @return the new builder.
     */
    public static JsonBuilder value(Boolean value) {
        return BuilderImpl.primitive(defaultBackend(), value, JsonBackend::of);
    }
}
