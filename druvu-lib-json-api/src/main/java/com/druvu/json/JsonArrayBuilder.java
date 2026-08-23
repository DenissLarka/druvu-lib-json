package com.druvu.json;

import java.time.temporal.Temporal;

/**
 * Builder for a JSON array. {@code add} methods refuse {@code null} values ({@link NullPointerException}): JSON
 * {@code null} is never built — an element you do not have is an element you do not add.
 *
 * @param <P> the enclosing builder that {@link #end()} returns to; the root builder returns itself.
 */
public sealed interface JsonArrayBuilder<P> extends JsonBuilder permits BuilderImpl {

    /**
     * Add a new object as the next element in this array.
     *
     * @return the builder for the new object.
     */
    JsonObjectBuilder<JsonArrayBuilder<P>> addObject();

    /**
     * Add a new array as the next element in this array.
     *
     * @return the builder for the new array.
     */
    JsonArrayBuilder<JsonArrayBuilder<P>> addArray();

    /**
     * End the current array and return to building the parent element.
     *
     * @return the parent builder.
     */
    P end();

    /**
     * Add a single value to this array.
     *
     * @param value the value to add.
     * @return the current builder.
     */
    JsonArrayBuilder<P> add(Boolean value);

    /**
     * Add a single value to this array.
     *
     * @param value the value to add.
     * @return the current builder.
     */
    JsonArrayBuilder<P> add(Number value);

    /**
     * Add a single value to this array.
     *
     * @param value the value to add.
     * @return the current builder.
     */
    JsonArrayBuilder<P> add(String value);

    /**
     * Add a date/time value, ISO-formatted.
     *
     * @param value the value to add.
     * @return the current builder.
     */
    JsonArrayBuilder<P> add(Temporal value);

    /**
     * Add a new array of elements as the next element in this array.
     *
     * @param builders the builders to get the elements from.
     * @return the current builder.
     */
    JsonArrayBuilder<P> add(Iterable<? extends JsonBuilder> builders);

    /**
     * Add a collection of elements to the current array, flat.
     *
     * @param builders the builders to get the elements from.
     * @return the current builder.
     */
    JsonArrayBuilder<P> addAll(Iterable<? extends JsonBuilder> builders);

    /**
     * Add a single element.
     *
     * @param builder the builder for the element to add.
     * @return the current builder.
     */
    JsonArrayBuilder<P> add(JsonBuilder builder);

    /**
     * Add an already parsed or built value as the next element. The value must come from the same backend as this
     * builder.
     *
     * @param value the value to add.
     * @return the current builder.
     */
    JsonArrayBuilder<P> add(JsonValue value);

    /**
     * Add a new array of mapped objects as the next element in this array.
     *
     * @param transform the transformer for the objects.
     * @param objects the objects to add.
     * @param <T> the type of the objects.
     * @return the current builder.
     */
    <T> JsonArrayBuilder<P> add(Mapper<T> transform, Iterable<T> objects);

    /**
     * Add a collection of mapped objects to the current array, flat.
     *
     * @param transform the transformer for the objects.
     * @param objects the objects to add.
     * @param <T> the type of the objects.
     * @return the current builder.
     */
    <T> JsonArrayBuilder<P> addAll(Mapper<T> transform, Iterable<T> objects);

    /**
     * Add each mapped object as an element of this array, flat — unlike {@link #add(Mapper, Iterable)}, which nests
     * them in a new array.
     *
     * @param transform the transformer for the objects.
     * @param objects the objects to add.
     * @param <T> the type of the objects.
     * @return the current builder.
     */
    <T> JsonArrayBuilder<P> add(Mapper<T> transform, T... objects);
}
