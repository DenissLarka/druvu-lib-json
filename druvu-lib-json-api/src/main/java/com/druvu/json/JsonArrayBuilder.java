package com.druvu.json;

import java.time.temporal.Temporal;
import java.util.function.Consumer;

/**
 * Builder for a JSON array: the elements a {@link JsonArrayBody} writes, the two chained nesting forms that
 * {@link #end()} returns from, and the document operations of {@link JsonBuilder}. {@code add} methods refuse
 * {@code null} values ({@link NullPointerException}): JSON {@code null} is never built — an element you do not have is
 * an element you do not add.
 *
 * @param <P> the enclosing builder that {@link #end()} returns to; the root builder returns itself.
 */
public sealed interface JsonArrayBuilder<P> extends JsonBuilder, JsonArrayBody permits BuilderImpl {

    /**
     * Add a new object as the next element in this array, chained: {@link #end()} on the returned builder comes back
     * here.
     *
     * @return the builder for the new object.
     */
    JsonObjectBuilder<JsonArrayBuilder<P>> addObject();

    /**
     * Add a new array as the next element in this array, chained: {@link #end()} on the returned builder comes back
     * here.
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

    @Override
    JsonArrayBuilder<P> addObject(Consumer<? super JsonObjectBody> body);

    @Override
    JsonArrayBuilder<P> addArray(Consumer<? super JsonArrayBody> body);

    @Override
    JsonArrayBuilder<P> add(Boolean value);

    @Override
    JsonArrayBuilder<P> add(Number value);

    @Override
    JsonArrayBuilder<P> add(String value);

    @Override
    JsonArrayBuilder<P> add(Temporal value);

    @Override
    JsonArrayBuilder<P> add(Iterable<? extends JsonBuilder> builders);

    @Override
    JsonArrayBuilder<P> addAll(Iterable<? extends JsonBuilder> builders);

    @Override
    JsonArrayBuilder<P> add(JsonBuilder builder);

    @Override
    JsonArrayBuilder<P> add(JsonValue value);

    @Override
    <T> JsonArrayBuilder<P> add(Mapper<T> transform, Iterable<T> objects);

    @Override
    <T> JsonArrayBuilder<P> addAll(Mapper<T> transform, Iterable<T> objects);

    @Override
    <T> JsonArrayBuilder<P> add(Mapper<T> transform, T... objects);
}
