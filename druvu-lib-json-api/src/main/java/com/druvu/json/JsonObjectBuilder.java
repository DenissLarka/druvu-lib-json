package com.druvu.json;

import java.time.temporal.Temporal;

/**
 * Builder for a JSON object. {@code add} methods refuse {@code null} values ({@link NullPointerException}): JSON
 * {@code null} is never built — a member you do not have is a member you do not add.
 *
 * @param <P> the enclosing builder that {@link #end()} returns to; the root builder returns itself.
 */
public sealed interface JsonObjectBuilder<P> extends JsonBuilder permits BuilderImpl {

    /**
     * Add a new object to this object under the specified key.
     *
     * @param key the key.
     * @return the builder for the new object.
     */
    JsonObjectBuilder<JsonObjectBuilder<P>> addObject(String key);

    /**
     * Add a new array to this object under the specified key.
     *
     * @param key the key.
     * @return the builder for the new array.
     */
    JsonArrayBuilder<JsonObjectBuilder<P>> addArray(String key);

    /**
     * End the current object and return to building the parent element.
     *
     * @return the parent builder.
     */
    P end();

    /**
     * Add an array of elements under a key.
     *
     * @param key the key for the new element.
     * @param builders the builders for the elements.
     * @return the current builder.
     */
    JsonObjectBuilder<P> add(String key, Iterable<? extends JsonBuilder> builders);

    /**
     * Add an array of mapped objects under a key.
     *
     * @param key the key for the new element.
     * @param transform the transformer for the objects.
     * @param objects the objects to add.
     * @param <T> the type of the objects.
     * @return the current builder.
     */
    <T> JsonObjectBuilder<P> add(String key, Mapper<T> transform, Iterable<T> objects);

    /**
     * Add a single element under a key.
     *
     * @param key the key for the new element.
     * @param builder the builder for the element.
     * @return the current builder.
     */
    JsonObjectBuilder<P> add(String key, JsonBuilder builder);

    /**
     * Add an already parsed or built value under a key. The value must come from the same backend as this builder.
     *
     * @param key the key for the new element.
     * @param value the value to add.
     * @return the current builder.
     */
    JsonObjectBuilder<P> add(String key, JsonValue value);

    /**
     * Add a single mapped object under a key.
     *
     * <p>To assign several objects as an array, use {@link #add(String, Mapper, Iterable)} — a key holds one value, so
     * this method takes one object.
     *
     * @param key the key for the new element.
     * @param transform the transformer for the object.
     * @param object the object to add.
     * @param <T> the object's type.
     * @return the current builder.
     */
    <T> JsonObjectBuilder<P> add(String key, Mapper<T> transform, T object);

    /**
     * Add a simple property.
     *
     * @param key the key for the property.
     * @param value the value to assign.
     * @return the current builder.
     */
    JsonObjectBuilder<P> add(String key, Boolean value);

    /**
     * Add a simple property.
     *
     * @param key the key for the property.
     * @param value the value to assign.
     * @return the current builder.
     */
    JsonObjectBuilder<P> add(String key, Number value);

    /**
     * Add a simple property.
     *
     * @param key the key for the property.
     * @param value the value to assign.
     * @return the current builder.
     */
    JsonObjectBuilder<P> add(String key, String value);

    /**
     * Add a date/time property, ISO-formatted.
     *
     * @param key the key for the property.
     * @param value the value to assign.
     * @return the current builder.
     */
    JsonObjectBuilder<P> add(String key, Temporal value);
}
