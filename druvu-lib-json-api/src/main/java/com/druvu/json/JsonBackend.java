package com.druvu.json;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.math.BigDecimal;
import java.time.temporal.Temporal;
import java.util.Set;

/**
 * SPI contract for JSON backends: node creation and mutation for the builder side, parsing and inspection for the
 * reading side, serialization for both.
 *
 * <p>Implement this interface and register it via a {@code com.druvu.lib.loader.ComponentFactory} service to add a new
 * backend. Users never touch this type unless they pass a backend explicitly to {@link Json}'s entry points.
 *
 * <p>Contracts the API layer upholds, so implementations may rely on them:
 *
 * <ul>
 *   <li>The {@code of} methods never receive {@code null}; the builder side refuses null values upstream and never
 *       builds JSON {@code null}.
 *   <li>{@link #asString}, {@link #asDecimal} and {@link #asBoolean} are only called on nodes whose {@link #kindOf}
 *       reported the matching kind.
 *   <li>{@link #member}, {@link #keys} are only called on OBJECT nodes; {@link #size}, {@link #element} only on ARRAY
 *       nodes, with {@code index} in bounds.
 * </ul>
 *
 * <p>Backends without a native date/time node type can rely on the default {@link #of(Temporal)}, which converts to an
 * ISO-formatted string. Backends with richer node types may override it.
 *
 * @param <N> the backend's native node type (e.g. {@code JsonElement} for Gson, {@code JsonNode} for Jackson)
 */
public interface JsonBackend<N> {

    // -- building ----------------------------------------------------------

    N newObject();

    N newArray();

    N of(String value);

    N of(Number value);

    N of(Boolean value);

    default N of(Temporal value) {
        return of(value.toString());
    }

    void setProperty(N objectNode, String key, N value);

    void addElement(N arrayNode, N element);

    // -- reading -----------------------------------------------------------

    /**
     * Parse a complete JSON document, strictly: reject malformed input and trailing content after the document. Errors
     * surface as the backend's own exceptions; the API layer wraps them in {@link JsonException}.
     *
     * @param in the input to parse.
     * @return the document's root node.
     * @throws IOException if reading fails.
     */
    N parse(Reader in) throws IOException;

    /**
     * @param node the node to classify.
     * @return the JSON kind of a node.
     */
    JsonValue.Kind kindOf(N node);

    /**
     * @param node a node of kind STRING.
     * @return the string it holds.
     */
    String asString(N node);

    /**
     * @param node a node of kind NUMBER.
     * @return the number it holds, exactly — no round-trip through floating point.
     */
    BigDecimal asDecimal(N node);

    /**
     * @param node a node of kind BOOLEAN.
     * @return the boolean it holds.
     */
    boolean asBoolean(N node);

    /**
     * @param objectNode a node of kind OBJECT.
     * @param key the member key.
     * @return the member under the key, or {@code null} when the key is absent. A key explicitly set to JSON
     *     {@code null} returns the backend's null node, keeping absence and null distinguishable.
     */
    N member(N objectNode, String key);

    /**
     * @param objectNode a node of kind OBJECT.
     * @return the member keys, in document order where the backend preserves it.
     */
    Set<String> keys(N objectNode);

    /**
     * @param arrayNode a node of kind ARRAY.
     * @return the number of elements.
     */
    int size(N arrayNode);

    /**
     * @param arrayNode a node of kind ARRAY.
     * @param index the element index, in bounds.
     * @return the element at the index.
     */
    N element(N arrayNode, int index);

    // -- output ------------------------------------------------------------

    String serialize(N node);

    void write(N node, Writer out) throws IOException;
}
