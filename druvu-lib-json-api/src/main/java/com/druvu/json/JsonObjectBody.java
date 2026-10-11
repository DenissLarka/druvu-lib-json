package com.druvu.json;

import java.time.temporal.Temporal;
import java.util.function.Consumer;

/**
 * The members of a JSON object as a lambda body writes them: the {@code add} methods, and the two nesting forms that
 * each take a body of their own. Nothing else — no {@code end()}, no {@code build()}: a body writes members, and the
 * builder that opened it finishes the document, so a missed {@code end()} cannot misplace a member.
 *
 * <pre>{@code
 * Json.object()
 *     .add("jsonrpc", "2.0")
 *     .addObject("error", e -> e.add("code", -32601).add("message", "Method not found"))
 *     .toJson();
 * }</pre>
 *
 * @author Deniss Larka <br>
 *     on 11 Oct 2026
 */
public sealed interface JsonObjectBody permits JsonObjectBuilder {

    /**
     * Add an object under a key and write its members in the body.
     *
     * @param key the key.
     * @param body writes the new object's members.
     * @return this body, at the same level.
     */
    JsonObjectBody addObject(String key, Consumer<? super JsonObjectBody> body);

    /**
     * Add an array under a key and write its elements in the body.
     *
     * @param key the key.
     * @param body writes the new array's elements.
     * @return this body, at the same level.
     */
    JsonObjectBody addArray(String key, Consumer<? super JsonArrayBody> body);

    /**
     * Add an array of elements under a key.
     *
     * @param key the key for the new element.
     * @param builders the builders for the elements.
     * @return this body.
     */
    JsonObjectBody add(String key, Iterable<? extends JsonBuilder> builders);

    /**
     * Add an array of mapped objects under a key.
     *
     * @param key the key for the new element.
     * @param transform the transformer for the objects.
     * @param objects the objects to add.
     * @param <T> the type of the objects.
     * @return this body.
     */
    <T> JsonObjectBody add(String key, Mapper<T> transform, Iterable<T> objects);

    /**
     * Add a single element under a key.
     *
     * @param key the key for the new element.
     * @param builder the builder for the element.
     * @return this body.
     */
    JsonObjectBody add(String key, JsonBuilder builder);

    /**
     * Add an already parsed or built value under a key. The value must come from the same backend as this builder.
     *
     * @param key the key for the new element.
     * @param value the value to add.
     * @return this body.
     */
    JsonObjectBody add(String key, JsonValue value);

    /**
     * Add a single mapped object under a key.
     *
     * @param key the key for the new element.
     * @param transform the transformer for the object.
     * @param object the object to add.
     * @param <T> the object's type.
     * @return this body.
     */
    <T> JsonObjectBody add(String key, Mapper<T> transform, T object);

    /**
     * Add a simple property.
     *
     * @param key the key for the property.
     * @param value the value to assign.
     * @return this body.
     */
    JsonObjectBody add(String key, Boolean value);

    /**
     * Add a simple property.
     *
     * @param key the key for the property.
     * @param value the value to assign.
     * @return this body.
     */
    JsonObjectBody add(String key, Number value);

    /**
     * Add a simple property.
     *
     * @param key the key for the property.
     * @param value the value to assign.
     * @return this body.
     */
    JsonObjectBody add(String key, String value);

    /**
     * Add a date/time property, ISO-formatted.
     *
     * @param key the key for the property.
     * @param value the value to assign.
     * @return this body.
     */
    JsonObjectBody add(String key, Temporal value);
}
