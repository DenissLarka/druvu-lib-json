package com.druvu.json;

import com.druvu.lib.loader.ComponentLoader;
import com.druvu.lib.loader.TargetClassNotFoundException;
import java.io.IOException;
import java.io.Writer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.temporal.Temporal;
import java.util.Date;
import java.util.TimeZone;
import java.util.function.BiFunction;

public class JsonBuilderFactory {

    /**
     * SPI contract for serialization backends.
     *
     * <p>Implement this interface and register it via a {@code com.druvu.lib.loader.ComponentFactory} service to add a
     * new backend. The {@code of} methods never receive {@code null}; null values are turned into {@link #nullNode()}
     * by the builders.
     *
     * <p>Backends without native date/time or character node types can rely on the default methods, which convert those
     * values to ISO-formatted strings. Backends with richer node types (e.g. native timestamps) may override them.
     *
     * @param <N> the backend's native node type (e.g. {@code JsonElement} for Gson, {@code JsonNode} for Jackson)
     */
    public interface Backend<N> {

        N newObject();

        N newArray();

        N nullNode();

        N of(String value);

        N of(Number value);

        N of(Boolean value);

        default N of(Character value) {
            return of(String.valueOf(value));
        }

        default N of(Temporal value) {
            return of(value.toString());
        }

        default N of(Date value) {
            DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'");
            dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
            return of(dateFormat.format(value));
        }

        void setProperty(N objectNode, String key, N value);

        void addElement(N arrayNode, N element);

        String serialize(N node);

        void write(N node, Writer out) throws IOException;
    }

    private static volatile Backend<?> defaultBackend;

    /**
     * Lazily discovers the backend, so that explicit-backend usage works without any backend registered. A failed
     * discovery is not cached: it throws again on the next call, with the original cause attached.
     */
    static Backend<?> defaultBackend() {
        Backend<?> backend = defaultBackend;
        if (backend == null) {
            synchronized (JsonBuilderFactory.class) {
                backend = defaultBackend;
                if (backend == null) {
                    try {
                        backend = ComponentLoader.load(Backend.class);
                    } catch (TargetClassNotFoundException e) {
                        throw new IllegalStateException(
                                "No JSON backend found. Add a backend module (e.g. com.druvu:fluent-json-gson) "
                                        + "to the classpath, or pass a Backend to the factory methods explicitly.",
                                e);
                    }
                    defaultBackend = backend;
                }
            }
        }
        return backend;
    }

    /** @return Start building a new json object, using the discovered backend. */
    public static JsonObjectBuilder<?, ?> buildObject() {
        return buildObject(defaultBackend());
    }

    /**
     * @param backend The backend to build with.
     * @param <N> The backend's node type.
     * @return Start building a new json object.
     */
    public static <N> JsonObjectBuilder<?, N> buildObject(Backend<N> backend) {
        N o = backend.newObject();
        return new Impl<>(backend, o, o);
    }

    /** @return Start building a new json array, using the discovered backend. */
    public static JsonArrayBuilder<?, ?> buildArray() {
        return buildArray(defaultBackend());
    }

    /**
     * @param backend The backend to build with.
     * @param <N> The backend's node type.
     * @return Start building a new json array.
     */
    public static <N> JsonArrayBuilder<?, N> buildArray(Backend<N> backend) {
        N a = backend.newArray();
        return new Impl<>(backend, a, a);
    }

    /**
     * @param transform The transformer for the objects
     * @param objects The objects to build
     * @param <T> The type of the objects
     * @return Start building a new json array, using the discovered backend.
     */
    public static <T> JsonArrayBuilder<?, ?> buildArray(Mapper<T> transform, Iterable<T> objects) {
        JsonArrayBuilder<?, ?> array = buildArray();
        for (T o : objects) {
            array.add(transform.map(o));
        }
        return array;
    }

    /**
     * Build a primitive for use in arrays, using the discovered backend.
     *
     * @param o The value
     * @return the new builder
     */
    public static JsonBuilder buildPrimitive(Number o) {
        return primitive(defaultBackend(), o, Backend::of);
    }

    /**
     * Build a primitive for use in arrays, using the discovered backend.
     *
     * @param o The value
     * @return the new builder
     */
    public static JsonBuilder buildPrimitive(Character o) {
        return primitive(defaultBackend(), o, Backend::of);
    }

    /**
     * Build a primitive for use in arrays, using the discovered backend.
     *
     * @param o The value
     * @return the new builder
     */
    public static JsonBuilder buildPrimitive(Boolean o) {
        return primitive(defaultBackend(), o, Backend::of);
    }

    /**
     * Build a primitive for use in arrays, using the discovered backend.
     *
     * @param o The value
     * @return the new builder
     */
    public static JsonBuilder buildPrimitive(String o) {
        return primitive(defaultBackend(), o, Backend::of);
    }

    private static <N, T> JsonBuilder primitive(Backend<N> backend, T value, BiFunction<Backend<N>, T, N> of) {
        N node = value == null ? backend.nullNode() : of.apply(backend, value);
        return new Impl<>(backend, node, node);
    }

    private static class Impl<N, P> implements JsonObjectBuilder<P, N>, JsonArrayBuilder<P, N> {

        private final Backend<N> backend;
        private final N root;
        private final N context;
        private final P parent;

        Impl(Backend<N> backend, N root, N context) {
            this.backend = backend;
            this.root = root;
            this.context = context;
            this.parent = self();
        }

        Impl(Backend<N> backend, N root, P parent, N context) {
            this.backend = backend;
            this.root = root;
            this.parent = parent;
            this.context = context;
        }

        @SuppressWarnings("unchecked")
        private P self() {
            return (P) this;
        }

        private <T> N node(T value, BiFunction<Backend<N>, T, N> of) {
            return value == null ? backend.nullNode() : of.apply(backend, value);
        }

        /**
         * Extracts the native node from another builder, refusing nodes that belong to a different backend — they would
         * corrupt this builder's tree.
         */
        private N graft(JsonBuilder builder) {
            if (!(builder instanceof Impl)) {
                throw new IllegalArgumentException("Unsupported JsonBuilder implementation: "
                        + builder.getClass().getName());
            }
            Impl<?, ?> other = (Impl<?, ?>) builder;
            if (other.backend.getClass() != backend.getClass()) {
                throw new IllegalArgumentException(String.format(
                        "Cannot mix backends: this builder uses %s, the added builder uses %s",
                        backend.getClass().getName(), other.backend.getClass().getName()));
            }
            @SuppressWarnings("unchecked")
            N node = (N) other.context;
            return node;
        }

        private Impl<N, ?> newArrayBuilder() {
            N a = backend.newArray();
            return new Impl<>(backend, a, a);
        }

        @Override
        public JsonObjectBuilder<JsonObjectBuilder<P, N>, N> addObject(String key) {
            N o = backend.newObject();
            backend.setProperty(context, key, o);
            return new Impl<>(backend, root, this, o);
        }

        @Override
        public JsonArrayBuilder<JsonObjectBuilder<P, N>, N> addArray(String key) {
            N a = backend.newArray();
            backend.setProperty(context, key, a);
            return new Impl<>(backend, root, this, a);
        }

        @Override
        public JsonObjectBuilder<JsonArrayBuilder<P, N>, N> addObject() {
            N o = backend.newObject();
            backend.addElement(context, o);
            return new Impl<>(backend, root, this, o);
        }

        @Override
        public JsonArrayBuilder<JsonArrayBuilder<P, N>, N> addArray() {
            N a = backend.newArray();
            backend.addElement(context, a);
            return new Impl<>(backend, root, this, a);
        }

        @Override
        public P end() {
            return parent;
        }

        @Override
        public JsonArrayBuilder<P, N> add(Iterable<? extends JsonBuilder> builders) {
            Impl<N, ?> array = newArrayBuilder();
            for (JsonBuilder b : builders) {
                array.add(b);
            }
            backend.addElement(context, array.context);
            return this;
        }

        @Override
        public JsonObjectBuilder<P, N> add(String key, Iterable<? extends JsonBuilder> builders) {
            Impl<N, ?> array = newArrayBuilder();
            for (JsonBuilder b : builders) {
                array.add(b);
            }
            backend.setProperty(context, key, array.context);
            return this;
        }

        @Override
        public JsonObjectBuilder<P, N> add(String key, Boolean value) {
            backend.setProperty(context, key, node(value, Backend::of));
            return this;
        }

        @Override
        public JsonObjectBuilder<P, N> add(String key, Character value) {
            backend.setProperty(context, key, node(value, Backend::of));
            return this;
        }

        @Override
        public JsonObjectBuilder<P, N> add(String key, Number value) {
            backend.setProperty(context, key, node(value, Backend::of));
            return this;
        }

        @Override
        public JsonObjectBuilder<P, N> add(String key, String value) {
            backend.setProperty(context, key, node(value, Backend::of));
            return this;
        }

        @Override
        public JsonObjectBuilder<P, N> add(String key, Temporal value) {
            backend.setProperty(context, key, node(value, Backend::of));
            return this;
        }

        @Override
        public JsonObjectBuilder<P, N> add(String key, Date value) {
            backend.setProperty(context, key, node(value, Backend::of));
            return this;
        }

        @Override
        public JsonObjectBuilder<P, N> addNull(String key) {
            backend.setProperty(context, key, backend.nullNode());
            return this;
        }

        @Override
        public JsonArrayBuilder<P, N> add(JsonBuilder builder) {
            backend.addElement(context, graft(builder));
            return this;
        }

        @Override
        public JsonObjectBuilder<P, N> add(String key, JsonBuilder builder) {
            backend.setProperty(context, key, graft(builder));
            return this;
        }

        @Override
        public JsonArrayBuilder<P, N> add(Boolean value) {
            backend.addElement(context, node(value, Backend::of));
            return this;
        }

        @Override
        public JsonArrayBuilder<P, N> add(Character value) {
            backend.addElement(context, node(value, Backend::of));
            return this;
        }

        @Override
        public JsonArrayBuilder<P, N> add(Number value) {
            backend.addElement(context, node(value, Backend::of));
            return this;
        }

        @Override
        public JsonArrayBuilder<P, N> add(String value) {
            backend.addElement(context, node(value, Backend::of));
            return this;
        }

        @Override
        public JsonArrayBuilder<P, N> add(Date value) {
            backend.addElement(context, node(value, Backend::of));
            return this;
        }

        @Override
        public JsonArrayBuilder<P, N> add(Temporal value) {
            backend.addElement(context, node(value, Backend::of));
            return this;
        }

        @Override
        public JsonArrayBuilder<P, N> addNull() {
            backend.addElement(context, backend.nullNode());
            return this;
        }

        @Override
        public <T> JsonObjectBuilder<P, N> add(String key, Mapper<T> transform, Iterable<T> objects) {
            Impl<N, ?> array = newArrayBuilder();
            for (T o : objects) {
                array.add(transform.map(o));
            }
            backend.setProperty(context, key, array.context);
            return this;
        }

        @Override
        public <T> JsonObjectBuilder<P, N> add(String key, Mapper<T> transform, T object) {
            return add(key, transform.map(object));
        }

        @Override
        public <T> JsonArrayBuilder<P, N> add(Mapper<T> transform, Iterable<T> objects) {
            Impl<N, ?> array = newArrayBuilder();
            for (T o : objects) {
                array.add(transform.map(o));
            }
            backend.addElement(context, array.context);
            return this;
        }

        @SafeVarargs
        @Override
        public final <T> JsonArrayBuilder<P, N> add(Mapper<T> transform, T... objects) {
            for (T object : objects) {
                add(transform.map(object));
            }
            return this;
        }

        @Override
        public JsonArrayBuilder<P, N> addAll(Iterable<? extends JsonBuilder> builders) {
            for (JsonBuilder o : builders) {
                add(o);
            }
            return this;
        }

        @Override
        public <T> JsonArrayBuilder<P, N> addAll(Mapper<T> transform, Iterable<T> objects) {
            for (T o : objects) {
                add(transform.map(o));
            }
            return this;
        }

        @Override
        public N getJson() {
            return root;
        }

        @Override
        public void write(Writer out) throws IOException {
            backend.write(root, out);
        }

        @Override
        public String toString() {
            return backend.serialize(root);
        }
    }
}
