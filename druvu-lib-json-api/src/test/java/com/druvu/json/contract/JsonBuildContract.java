package com.druvu.json.contract;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.druvu.json.Json;
import com.druvu.json.JsonArrayBuilder;
import com.druvu.json.JsonBackend;
import com.druvu.json.JsonBuilder;
import com.druvu.json.JsonObject;
import com.druvu.json.JsonValue;
import com.druvu.json.Mapper;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import org.testng.annotations.Test;

/**
 * The building side of the contract: fluent construction, mappers, temporals, the null doctrine on write, and the
 * backend-mixing guard. Every expectation is structural.
 *
 * @author Deniss Larka
 */
public abstract class JsonBuildContract extends JsonContract {

    /** A domain object to map. */
    protected record Sample(String b, String c) {
        Sample() {
            this("hello", "world");
        }
    }

    private final Mapper<Sample> sampleMapper = s -> object().add("b", s.b()).add("c", s.c());

    /** Maps a plain string through the front door under test — a mapper is always bound to one. */
    private final Mapper<String> stringMapper = this::value;

    private static final String TWO_SAMPLES = "[{\"b\":\"hello\",\"c\":\"world\"},{\"b\":\"hello\",\"c\":\"world\"}]";

    @Test
    public void buildsNestedObject() {
        JsonValue built = object().add("string", "1")
                .add("number", 2)
                .add("boolean", true)
                .addObject("obj")
                .add("NP1", 4)
                .end()
                .addArray("arr")
                .addObject()
                .end()
                .add("AE1")
                .end()
                .build();

        assertSameTree(
                built,
                parse("{\"string\":\"1\",\"number\":2,\"boolean\":true,\"obj\":{\"NP1\":4},\"arr\":[{},\"AE1\"]}"));
    }

    @Test
    public void buildsNestedArray() {
        JsonValue built = array().add("1")
                .add(2)
                .add(true)
                .addObject()
                .add("NP1", 4)
                .end()
                .addArray()
                .addObject()
                .end()
                .add("AE1")
                .end()
                .build();

        assertSameTree(built, parse("[\"1\",2,true,{\"NP1\":4},[{},\"AE1\"]]"));
    }

    @Test
    public void nullValuesRefusedOnEveryWriteDoor() {
        assertThatThrownBy(() -> object().add("key", JsonBuildContract.<String>unset()))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("omit");
        assertThatThrownBy(() -> object().add("key", JsonBuildContract.<Number>unset()))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> array().add(JsonBuildContract.<Boolean>unset()))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> value(JsonBuildContract.<String>unset())).isInstanceOf(NullPointerException.class);
    }

    @Test
    public void temporalsBuildAsIsoStrings() {
        JsonObject built = object().add("instant", Instant.ofEpochMilli(0))
                .add("date", LocalDate.of(0, 1, 1))
                .build()
                .asObject();

        assertThat(built.string("instant")).isEqualTo("1970-01-01T00:00:00Z");
        assertThat(built.string("date")).isEqualTo("0000-01-01");
    }

    @Test
    public void standalonePrimitivesBuild() {
        assertThat(value(42).build().asInt()).isEqualTo(42);
        assertThat(value("x").build().asString()).isEqualTo("x");
        assertThat(value(true).build().asBoolean()).isTrue();
    }

    @Test
    public void bigDecimalStaysExact() {
        BigDecimal exact = new BigDecimal("0.1000000000000000055511151231257827");
        JsonObject built = object().add("v", exact).build().asObject();
        assertThat(built.decimal("v")).isEqualTo(exact);
    }

    @Test
    public void bigDecimalScaleSurvivesBuilding() {
        // 2.50 is not 2.5 to a money caller; the tree keeps the value exactly as given.
        JsonObject built = object().add("n", new BigDecimal("2.50")).build().asObject();
        assertThat(built.decimal("n")).isEqualTo(new BigDecimal("2.50"));
    }

    @Test
    public void mapsObjectIntoObject() {
        JsonValue built = object().add("value", sampleMapper, new Sample()).build();
        assertSameTree(built, parse("{\"value\":{\"b\":\"hello\",\"c\":\"world\"}}"));
    }

    @Test
    public void mapsIterableIntoObjectAsArray() {
        JsonValue built = object().add("value", sampleMapper, List.of(new Sample(), new Sample()))
                .build();
        assertSameTree(built, parse("{\"value\":" + TWO_SAMPLES + "}"));
    }

    @Test
    public void mapsObjectIntoArray() {
        JsonValue built = array().add(sampleMapper, new Sample()).build();
        assertSameTree(built, parse("[{\"b\":\"hello\",\"c\":\"world\"}]"));
    }

    @Test
    public void mapsIterableIntoArrayNested() {
        JsonValue built =
                array().add(sampleMapper, List.of(new Sample(), new Sample())).build();
        assertSameTree(built, parse("[" + TWO_SAMPLES + "]"));
    }

    @Test
    public void mapsIterableIntoArrayFlatViaAddAll() {
        JsonValue built = array().addAll(sampleMapper, List.of(new Sample(), new Sample()))
                .build();
        assertSameTree(built, parse(TWO_SAMPLES));
    }

    @Test
    public void frontDoorMapsIterableIntoArrayFlat() {
        JsonValue built =
                array(sampleMapper, List.of(new Sample(), new Sample())).build();
        assertSameTree(built, parse(TWO_SAMPLES));
    }

    @Test
    public void addsBuildersIntoArrayNested() {
        JsonValue built = array().add(twoSampleBuilders()).build();
        assertSameTree(built, parse("[" + TWO_SAMPLES + "]"));
    }

    @Test
    public void addsBuildersIntoObjectAsArray() {
        JsonValue built = object().add("val", twoSampleBuilders()).build();
        assertSameTree(built, parse("{\"val\":" + TWO_SAMPLES + "}"));
    }

    @Test
    public void addsAllBuildersIntoArrayFlat() {
        JsonValue built = array().addAll(twoSampleBuilders()).build();
        assertSameTree(built, parse(TWO_SAMPLES));
    }

    @Test
    public void addsArrayOfPrimitives() {
        JsonValue built = object().add("name", "Joe")
                .add("tastes", array().addAll(stringMapper, List.of("chicken", "pasta")))
                .build();
        assertSameTree(built, parse("{\"name\":\"Joe\",\"tastes\":[\"chicken\",\"pasta\"]}"));
    }

    /**
     * A key holds one value: the object-side Mapper overload takes a single object and assigns it as a scalar, while
     * the Iterable overload assigns an array.
     */
    @Test
    public void mapperSingleObjectVersusIterableOnObject() {
        assertSameTree(object().add("k", stringMapper, "a").build(), parse("{\"k\":\"a\"}"));
        assertSameTree(
                object().add("k", stringMapper, List.of("a", "b", "c")).build(), parse("{\"k\":[\"a\",\"b\",\"c\"]}"));
    }

    /**
     * On arrays both Mapper overloads remain, and they differ: varargs appends the mapped objects flat, the Iterable
     * overload nests them in a new array.
     */
    @Test
    public void mapperVarargsVersusIterableOnArray() {
        assertSameTree(array().add(stringMapper, "a", "b", "c").build(), parse("[\"a\",\"b\",\"c\"]"));
        assertSameTree(array().add(stringMapper, List.of("a", "b", "c")).build(), parse("[[\"a\",\"b\",\"c\"]]"));
    }

    @Test
    public void explicitBackendWorksWithoutDiscovery() {
        JsonBackend<?> backend = backend();
        JsonValue built = Json.object(backend)
                .add("name", "Alice")
                .add("tags", Json.array(backend).add("x"))
                .build();
        assertSameTree(built, Json.parse("{\"name\":\"Alice\",\"tags\":[\"x\"]}", backend));
    }

    @Test
    public void mixedBackendBuildersRejected() {
        JsonArrayBuilder<?> array = Json.array(delegating(backend()));
        JsonBuilder other = Json.object(backend()).add("a", 1);

        assertThatThrownBy(() -> array.add(other))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cannot mix backends");
    }

    @Test
    public void mixedBackendValuesRejected() {
        JsonValue foreign = Json.parse("{\"a\":1}", delegating(backend()));

        assertThatThrownBy(() -> Json.object(backend()).add("k", foreign))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cannot mix backends");
    }

    /** A null that arrives at runtime, as from a field nobody set — invisible to static null analysis by design. */
    private static <T> T unset() {
        return new AtomicReference<T>().get();
    }

    private List<JsonBuilder> twoSampleBuilders() {
        return List.of(
                object().add("b", "hello").add("c", "world"),
                object().add("b", "hello").add("c", "world"));
    }

    private static <N> JsonBackend<N> delegating(JsonBackend<N> delegate) {
        return new Delegating<>(delegate);
    }

    /** Behaves exactly like the wrapped backend but is a different class, which is what the mixing guard keys on. */
    private static final class Delegating<N> implements JsonBackend<N> {
        private final JsonBackend<N> delegate;

        Delegating(JsonBackend<N> delegate) {
            this.delegate = delegate;
        }

        @Override
        public N newObject() {
            return delegate.newObject();
        }

        @Override
        public N newArray() {
            return delegate.newArray();
        }

        @Override
        public N of(String value) {
            return delegate.of(value);
        }

        @Override
        public N of(Number value) {
            return delegate.of(value);
        }

        @Override
        public N of(Boolean value) {
            return delegate.of(value);
        }

        @Override
        public void setProperty(N objectNode, String key, N value) {
            delegate.setProperty(objectNode, key, value);
        }

        @Override
        public void addElement(N arrayNode, N element) {
            delegate.addElement(arrayNode, element);
        }

        @Override
        public N parse(Reader in) throws IOException {
            return delegate.parse(in);
        }

        @Override
        public JsonValue.Kind kindOf(N node) {
            return delegate.kindOf(node);
        }

        @Override
        public String asString(N node) {
            return delegate.asString(node);
        }

        @Override
        public BigDecimal asDecimal(N node) {
            return delegate.asDecimal(node);
        }

        @Override
        public boolean asBoolean(N node) {
            return delegate.asBoolean(node);
        }

        @Override
        public N member(N objectNode, String key) {
            return delegate.member(objectNode, key);
        }

        @Override
        public Set<String> keys(N objectNode) {
            return delegate.keys(objectNode);
        }

        @Override
        public int size(N arrayNode) {
            return delegate.size(arrayNode);
        }

        @Override
        public N element(N arrayNode, int index) {
            return delegate.element(arrayNode, index);
        }

        @Override
        public String serialize(N node) {
            return delegate.serialize(node);
        }

        @Override
        public void write(N node, Writer out) throws IOException {
            delegate.write(node, out);
        }
    }
}
