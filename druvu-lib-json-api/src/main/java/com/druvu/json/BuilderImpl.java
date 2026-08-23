package com.druvu.json;

import java.io.IOException;
import java.io.Writer;
import java.time.temporal.Temporal;
import java.util.function.BiFunction;

/**
 * The one-builder implementation behind both builder interfaces. Holds the backend untyped: the node type is an
 * implementation detail of the backend, never visible through the public API.
 *
 * @param <P> the enclosing builder that {@code end()} returns to.
 */
final class BuilderImpl<P> implements JsonObjectBuilder<P>, JsonArrayBuilder<P> {

    private final JsonBackend<Object> backend;
    private final Object root;
    private final Object context;
    private final P parent;

    private BuilderImpl(JsonBackend<Object> backend, Object root, Object context) {
        this.backend = backend;
        this.root = root;
        this.context = context;
        this.parent = self();
    }

    private BuilderImpl(JsonBackend<Object> backend, Object root, P parent, Object context) {
        this.backend = backend;
        this.root = root;
        this.parent = parent;
        this.context = context;
    }

    static JsonObjectBuilder<?> newObject(JsonBackend<?> backend) {
        JsonBackend<Object> b = cast(backend);
        Object o = b.newObject();
        return new BuilderImpl<>(b, o, o);
    }

    static JsonArrayBuilder<?> newArray(JsonBackend<?> backend) {
        JsonBackend<Object> b = cast(backend);
        Object a = b.newArray();
        return new BuilderImpl<>(b, a, a);
    }

    static <T> JsonBuilder primitive(JsonBackend<?> backend, T value, BiFunction<JsonBackend<Object>, T, Object> of) {
        JsonBackend<Object> b = cast(backend);
        Object node = of.apply(b, refuseNull(value));
        return new BuilderImpl<>(b, node, node);
    }

    @SuppressWarnings("unchecked")
    private static JsonBackend<Object> cast(JsonBackend<?> backend) {
        return (JsonBackend<Object>) backend;
    }

    @SuppressWarnings("unchecked")
    private P self() {
        return (P) this;
    }

    private <T> Object node(T value, BiFunction<JsonBackend<Object>, T, Object> of) {
        return of.apply(backend, refuseNull(value));
    }

    private static <T> T refuseNull(T value) {
        if (value == null) {
            throw new NullPointerException(
                    "JSON null is never built — omit what you do not have instead of adding null");
        }
        return value;
    }

    /**
     * Extracts the native node from another builder, refusing nodes that belong to a different backend — they would
     * corrupt this builder's tree. The cast is total: the builder interfaces are sealed onto this class.
     */
    private Object graft(JsonBuilder builder) {
        BuilderImpl<?> other = (BuilderImpl<?>) builder;
        return checkSameBackend(other.backend, other.context);
    }

    /**
     * Extracts the native node from a parsed or built value, with the same backend guard as builder grafting. The cast
     * is total: {@link JsonValue} is sealed and every permitted class extends {@link BackedValue}.
     */
    private Object graft(JsonValue value) {
        BackedValue other = (BackedValue) value;
        return checkSameBackend(other.backend, other.node);
    }

    private Object checkSameBackend(JsonBackend<Object> otherBackend, Object node) {
        if (otherBackend.getClass() != backend.getClass()) {
            throw new IllegalArgumentException(String.format(
                    "Cannot mix backends: this builder uses %s, the added one uses %s",
                    backend.getClass().getName(), otherBackend.getClass().getName()));
        }
        return node;
    }

    private BuilderImpl<?> nestedArray() {
        Object a = backend.newArray();
        return new BuilderImpl<>(backend, a, a);
    }

    @Override
    public JsonObjectBuilder<JsonObjectBuilder<P>> addObject(String key) {
        Object o = backend.newObject();
        backend.setProperty(context, key, o);
        return new BuilderImpl<>(backend, root, this, o);
    }

    @Override
    public JsonArrayBuilder<JsonObjectBuilder<P>> addArray(String key) {
        Object a = backend.newArray();
        backend.setProperty(context, key, a);
        return new BuilderImpl<>(backend, root, this, a);
    }

    @Override
    public JsonObjectBuilder<JsonArrayBuilder<P>> addObject() {
        Object o = backend.newObject();
        backend.addElement(context, o);
        return new BuilderImpl<>(backend, root, this, o);
    }

    @Override
    public JsonArrayBuilder<JsonArrayBuilder<P>> addArray() {
        Object a = backend.newArray();
        backend.addElement(context, a);
        return new BuilderImpl<>(backend, root, this, a);
    }

    @Override
    public P end() {
        return parent;
    }

    @Override
    public JsonArrayBuilder<P> add(Iterable<? extends JsonBuilder> builders) {
        BuilderImpl<?> array = nestedArray();
        for (JsonBuilder builder : builders) {
            array.add(builder);
        }
        backend.addElement(context, array.context);
        return this;
    }

    @Override
    public JsonObjectBuilder<P> add(String key, Iterable<? extends JsonBuilder> builders) {
        BuilderImpl<?> array = nestedArray();
        for (JsonBuilder builder : builders) {
            array.add(builder);
        }
        backend.setProperty(context, key, array.context);
        return this;
    }

    @Override
    public JsonObjectBuilder<P> add(String key, Boolean value) {
        backend.setProperty(context, key, node(value, JsonBackend::of));
        return this;
    }

    @Override
    public JsonObjectBuilder<P> add(String key, Number value) {
        backend.setProperty(context, key, node(value, JsonBackend::of));
        return this;
    }

    @Override
    public JsonObjectBuilder<P> add(String key, String value) {
        backend.setProperty(context, key, node(value, JsonBackend::of));
        return this;
    }

    @Override
    public JsonObjectBuilder<P> add(String key, Temporal value) {
        backend.setProperty(context, key, node(value, JsonBackend::of));
        return this;
    }

    @Override
    public JsonArrayBuilder<P> add(JsonBuilder builder) {
        backend.addElement(context, graft(builder));
        return this;
    }

    @Override
    public JsonObjectBuilder<P> add(String key, JsonBuilder builder) {
        backend.setProperty(context, key, graft(builder));
        return this;
    }

    @Override
    public JsonArrayBuilder<P> add(JsonValue value) {
        backend.addElement(context, graft(value));
        return this;
    }

    @Override
    public JsonObjectBuilder<P> add(String key, JsonValue value) {
        backend.setProperty(context, key, graft(value));
        return this;
    }

    @Override
    public JsonArrayBuilder<P> add(Boolean value) {
        backend.addElement(context, node(value, JsonBackend::of));
        return this;
    }

    @Override
    public JsonArrayBuilder<P> add(Number value) {
        backend.addElement(context, node(value, JsonBackend::of));
        return this;
    }

    @Override
    public JsonArrayBuilder<P> add(String value) {
        backend.addElement(context, node(value, JsonBackend::of));
        return this;
    }

    @Override
    public JsonArrayBuilder<P> add(Temporal value) {
        backend.addElement(context, node(value, JsonBackend::of));
        return this;
    }

    @Override
    public <T> JsonObjectBuilder<P> add(String key, Mapper<T> transform, Iterable<T> objects) {
        BuilderImpl<?> array = nestedArray();
        for (T object : objects) {
            array.add(transform.map(object));
        }
        backend.setProperty(context, key, array.context);
        return this;
    }

    @Override
    public <T> JsonObjectBuilder<P> add(String key, Mapper<T> transform, T object) {
        return add(key, transform.map(object));
    }

    @Override
    public <T> JsonArrayBuilder<P> add(Mapper<T> transform, Iterable<T> objects) {
        BuilderImpl<?> array = nestedArray();
        for (T object : objects) {
            array.add(transform.map(object));
        }
        backend.addElement(context, array.context);
        return this;
    }

    @SafeVarargs
    @Override
    public final <T> JsonArrayBuilder<P> add(Mapper<T> transform, T... objects) {
        for (T object : objects) {
            add(transform.map(object));
        }
        return this;
    }

    @Override
    public JsonArrayBuilder<P> addAll(Iterable<? extends JsonBuilder> builders) {
        for (JsonBuilder builder : builders) {
            add(builder);
        }
        return this;
    }

    @Override
    public <T> JsonArrayBuilder<P> addAll(Mapper<T> transform, Iterable<T> objects) {
        for (T object : objects) {
            add(transform.map(object));
        }
        return this;
    }

    @Override
    public JsonValue build() {
        return BackedValue.wrap(backend, root, "$");
    }

    @Override
    public String toJson() {
        return backend.serialize(root);
    }

    @Override
    public void write(Writer out) throws IOException {
        backend.write(root, out);
    }

    @Override
    public String toString() {
        return toJson();
    }
}
