package com.druvu.json;

import java.time.temporal.Temporal;
import java.util.function.Consumer;

/**
 * Builder for a JSON object: the members a {@link JsonObjectBody} writes, the two chained nesting forms that
 * {@link #end()} returns from, and the document operations of {@link JsonBuilder}. {@code add} methods refuse
 * {@code null} values ({@link NullPointerException}): JSON {@code null} is never built — a member you do not have is a
 * member you do not add.
 *
 * @param <P> the enclosing builder that {@link #end()} returns to; the root builder returns itself.
 */
public sealed interface JsonObjectBuilder<P> extends JsonBuilder, JsonObjectBody permits BuilderImpl {

    /**
     * Add a new object to this object under the specified key, chained: {@link #end()} on the returned builder comes
     * back here.
     *
     * @param key the key.
     * @return the builder for the new object.
     */
    JsonObjectBuilder<JsonObjectBuilder<P>> addObject(String key);

    /**
     * Add a new array to this object under the specified key, chained: {@link #end()} on the returned builder comes
     * back here.
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

    @Override
    JsonObjectBuilder<P> addObject(String key, Consumer<? super JsonObjectBody> body);

    @Override
    JsonObjectBuilder<P> addArray(String key, Consumer<? super JsonArrayBody> body);

    @Override
    JsonObjectBuilder<P> add(String key, Iterable<? extends JsonBuilder> builders);

    @Override
    <T> JsonObjectBuilder<P> add(String key, Mapper<T> transform, Iterable<T> objects);

    @Override
    JsonObjectBuilder<P> add(String key, JsonBuilder builder);

    @Override
    JsonObjectBuilder<P> add(String key, JsonValue value);

    @Override
    <T> JsonObjectBuilder<P> add(String key, Mapper<T> transform, T object);

    @Override
    JsonObjectBuilder<P> add(String key, Boolean value);

    @Override
    JsonObjectBuilder<P> add(String key, Number value);

    @Override
    JsonObjectBuilder<P> add(String key, String value);

    @Override
    JsonObjectBuilder<P> add(String key, Temporal value);
}
