package com.druvu.json;

import java.time.temporal.Temporal;
import java.util.function.Consumer;

/**
 * The elements of a JSON array as a lambda body writes them: the {@code add} methods, and the two nesting forms that
 * each take a body of their own. Nothing else — no {@code end()}, no {@code build()}: a body writes elements, and the
 * builder that opened it finishes the document.
 *
 * @author Deniss Larka <br>
 *     on 11 Oct 2026
 */
public sealed interface JsonArrayBody permits JsonArrayBuilder {

    /**
     * Add an object as the next element and write its members in the body.
     *
     * @param body writes the new object's members.
     * @return this body, at the same level.
     */
    JsonArrayBody addObject(Consumer<? super JsonObjectBody> body);

    /**
     * Add an array as the next element and write its elements in the body.
     *
     * @param body writes the new array's elements.
     * @return this body, at the same level.
     */
    JsonArrayBody addArray(Consumer<? super JsonArrayBody> body);

    /**
     * Add a single value.
     *
     * @param value the value to add.
     * @return this body.
     */
    JsonArrayBody add(Boolean value);

    /**
     * Add a single value.
     *
     * @param value the value to add.
     * @return this body.
     */
    JsonArrayBody add(Number value);

    /**
     * Add a single value.
     *
     * @param value the value to add.
     * @return this body.
     */
    JsonArrayBody add(String value);

    /**
     * Add a date/time value, ISO-formatted.
     *
     * @param value the value to add.
     * @return this body.
     */
    JsonArrayBody add(Temporal value);

    /**
     * Add a new array of elements as the next element.
     *
     * @param builders the builders to get the elements from.
     * @return this body.
     */
    JsonArrayBody add(Iterable<? extends JsonBuilder> builders);

    /**
     * Add a collection of elements, flat.
     *
     * @param builders the builders to get the elements from.
     * @return this body.
     */
    JsonArrayBody addAll(Iterable<? extends JsonBuilder> builders);

    /**
     * Add a single element.
     *
     * @param builder the builder for the element to add.
     * @return this body.
     */
    JsonArrayBody add(JsonBuilder builder);

    /**
     * Add an already parsed or built value as the next element. The value must come from the same backend as this
     * builder.
     *
     * @param value the value to add.
     * @return this body.
     */
    JsonArrayBody add(JsonValue value);

    /**
     * Add a new array of mapped objects as the next element.
     *
     * @param transform the transformer for the objects.
     * @param objects the objects to add.
     * @param <T> the type of the objects.
     * @return this body.
     */
    <T> JsonArrayBody add(Mapper<T> transform, Iterable<T> objects);

    /**
     * Add a collection of mapped objects, flat.
     *
     * @param transform the transformer for the objects.
     * @param objects the objects to add.
     * @param <T> the type of the objects.
     * @return this body.
     */
    <T> JsonArrayBody addAll(Mapper<T> transform, Iterable<T> objects);

    /**
     * Add each mapped object as an element, flat — unlike {@link #add(Mapper, Iterable)}, which nests them in a new
     * array.
     *
     * @param transform the transformer for the objects.
     * @param objects the objects to add.
     * @param <T> the type of the objects.
     * @return this body.
     */
    <T> JsonArrayBody add(Mapper<T> transform, T... objects);
}
