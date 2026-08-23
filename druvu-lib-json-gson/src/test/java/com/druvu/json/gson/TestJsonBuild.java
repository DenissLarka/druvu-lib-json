/*
 *    Copyright 2013 Bryn Cooke
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.druvu.json.gson;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.druvu.json.Json;
import com.druvu.json.JsonArrayBuilder;
import com.druvu.json.JsonBackend;
import com.druvu.json.JsonBuilder;
import com.druvu.json.JsonValue;
import com.druvu.json.Mapper;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.testng.Assert;
import org.testng.annotations.Test;

public class TestJsonBuild {

    @Test
    public void testBuildObject() {
        Object built = Json.object()
                .add("string", "1")
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
                .build()
                .raw();

        JsonElement test = JsonParser.parseString(
                "{\"string\":\"1\",\"number\":2,\"boolean\":true,\"obj\":{\"NP1\":4},\"arr\":[{},\"AE1\"]}");
        assertThat(built).isEqualTo(test);
    }

    @Test
    public void nullValuesRefusedOnEveryWriteDoor() {
        assertThatThrownBy(() -> Json.object().add("key", (String) null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("omit");
        assertThatThrownBy(() -> Json.object().add("key", (Number) null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> Json.array().add((Boolean) null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> Json.value((String) null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    public void testSerialization() throws IOException {
        Instant instant = Instant.ofEpochMilli(0);
        LocalDate localDate = LocalDate.of(0, 1, 1);
        JsonBuilder builder = Json.object()
                .add("Prop1", "1")
                .add("Prop2", 2)
                .addObject("Prop5")
                .add("NP1", 4)
                .end()
                .addArray("Foo")
                .addObject()
                .end()
                .add("AE1")
                .end()
                .add("Prop6", instant)
                .add("Prop7", localDate);

        StringWriter writer = new StringWriter();
        builder.write(writer);
        String expected =
                "{\"Prop1\":\"1\",\"Prop2\":2,\"Prop5\":{\"NP1\":4},\"Foo\":[{},\"AE1\"],\"Prop6\":\"1970-01-01T00:00:00Z\",\"Prop7\":\"0000-01-01\"}";
        assertThat(builder.toJson()).isEqualTo(expected);
        assertThat(builder.toString()).isEqualTo(expected);
        assertThat(writer.toString()).isEqualTo(expected);
    }

    @Test
    public void testBuildArray() {
        Object built = Json.array()
                .add("1")
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
                .build()
                .raw();

        JsonElement test = JsonParser.parseString("[\"1\", 2, true,{\"NP1\":4},[{},\"AE1\"]]");
        assertThat(built).isEqualTo(test);
    }

    public static class A {
        String b = "hello";
        String c = "world";
    }

    private static final Mapper<A> A_MAPPER = t -> Json.object().add("b", t.b).add("c", t.c);

    @Test
    public void testTransformationObject() {
        Object built = Json.object().add("value", A_MAPPER, new A()).build().raw();
        JsonElement test = JsonParser.parseString("{\"value\":{\"b\":\"hello\",\"c\":\"world\"}}");
        Assert.assertEquals(test, built);
    }

    @Test
    public void testTransformationArrayInObject() {
        List<A> aList = Arrays.asList(new A(), new A());

        Object built = Json.object().add("value", A_MAPPER, aList).build().raw();
        JsonElement test = JsonParser.parseString(
                "{\"value\":[{\"b\":\"hello\",\"c\":\"world\"}, {\"b\":\"hello\",\"c\":\"world\"}]}");
        Assert.assertEquals(test, built);
    }

    @Test
    public void testTransformationObjectInArray() {
        Object built = Json.array().add(A_MAPPER, new A()).build().raw();
        JsonElement test = JsonParser.parseString("[{\"b\":\"hello\",\"c\":\"world\"}]");
        Assert.assertEquals(test, built);
    }

    @Test
    public void testTransformationArrayInArray() {
        List<A> aList = Arrays.asList(new A(), new A());

        Object built = Json.array().add(A_MAPPER, aList).build().raw();
        JsonElement test =
                JsonParser.parseString("[[{\"b\":\"hello\",\"c\":\"world\"}, {\"b\":\"hello\",\"c\":\"world\"}]]");
        Assert.assertEquals(test, built);
    }

    @Test
    public void testTransformationArray() {
        List<A> aList = Arrays.asList(new A(), new A());

        Object built = Json.array(A_MAPPER, aList).build().raw();
        JsonElement test =
                JsonParser.parseString("[{\"b\":\"hello\",\"c\":\"world\"}, {\"b\":\"hello\",\"c\":\"world\"}]");
        Assert.assertEquals(test, built);
    }

    @Test
    public void testAddJsonBuildersOnArray() {
        List<? extends JsonBuilder> aList = Arrays.asList(
                Json.object().add("b", "hello").add("c", "world"),
                Json.object().add("b", "hello").add("c", "world"));

        Object built = Json.array().add(aList).build().raw();
        JsonElement test =
                JsonParser.parseString("[[{\"b\":\"hello\",\"c\":\"world\"}, {\"b\":\"hello\",\"c\":\"world\"}]]");
        Assert.assertEquals(test, built);
    }

    @Test
    public void testAddJsonBuildersOnObject() {
        List<? extends JsonBuilder> aList = Arrays.asList(
                Json.object().add("b", "hello").add("c", "world"),
                Json.object().add("b", "hello").add("c", "world"));

        Object built = Json.object().add("val", aList).build().raw();
        JsonElement test = JsonParser.parseString(
                "{\"val\":[{\"b\":\"hello\",\"c\":\"world\"}, {\"b\":\"hello\",\"c\":\"world\"}]}");
        Assert.assertEquals(test, built);
    }

    @Test
    public void testAddAllJsonBuildersOnArray() {
        List<? extends JsonBuilder> aList = Arrays.asList(
                Json.object().add("b", "hello").add("c", "world"),
                Json.object().add("b", "hello").add("c", "world"));

        Object built = Json.array().addAll(aList).build().raw();
        JsonElement test =
                JsonParser.parseString("[{\"b\":\"hello\",\"c\":\"world\"}, {\"b\":\"hello\",\"c\":\"world\"}]");
        Assert.assertEquals(test, built);
    }

    @Test
    public void testTransformationArrayAddAll() {
        List<A> aList = Arrays.asList(new A(), new A());

        Object built = Json.array().addAll(A_MAPPER, aList).build().raw();
        JsonElement test =
                JsonParser.parseString("[{\"b\":\"hello\",\"c\":\"world\"}, {\"b\":\"hello\",\"c\":\"world\"}]");
        Assert.assertEquals(test, built);
    }

    @Test
    public void testAddArrayOfPrimitives() {
        List<String> likes = Arrays.asList("chicken", "pasta");
        String json = Json.object()
                .add("name", "Joe")
                .add("tastes", Json.array().addAll(Mapper.STRING, likes))
                .toJson();
        Assert.assertEquals("{\"name\":\"Joe\",\"tastes\":[\"chicken\",\"pasta\"]}", json);
    }

    @Test
    public void testPrimitiveBuilderSerializes() {
        assertThat(Json.value(42).toJson()).isEqualTo("42");
        assertThat(Json.value("x").toJson()).isEqualTo("\"x\"");
    }

    @Test
    public void testBigDecimalStaysExact() {
        Assert.assertEquals(
                "{\"v\":0.1000000000000000055511151231257827}",
                Json.object()
                        .add("v", new BigDecimal("0.1000000000000000055511151231257827"))
                        .toJson());
    }

    @Test
    public void testExplicitBackendRawInterop() {
        JsonElement json = (JsonElement) Json.object(new GsonBackend())
                .add("name", "Alice")
                .add("age", 30)
                .build()
                .raw();
        Assert.assertEquals(JsonParser.parseString("{\"name\":\"Alice\",\"age\":30}"), json);
    }

    @Test
    public void testMixedBackendsRejected() {
        JsonArrayBuilder<?> array = Json.array(new OtherBackend());
        JsonBuilder other = Json.object(new GsonBackend()).add("a", 1);

        IllegalArgumentException e = Assert.expectThrows(IllegalArgumentException.class, () -> array.add(other));
        Assert.assertTrue(e.getMessage().contains("Cannot mix backends"), e.getMessage());
    }

    @Test
    public void testMixedBackendValueRejected() {
        JsonValue foreign = Json.parse("{\"a\":1}", new OtherBackend());

        IllegalArgumentException e = Assert.expectThrows(
                IllegalArgumentException.class,
                () -> Json.object(new GsonBackend()).add("k", foreign));
        Assert.assertTrue(e.getMessage().contains("Cannot mix backends"), e.getMessage());
    }

    /** Behaves exactly like GsonBackend but is a different class, which is what the mixing guard keys on. */
    private static final class OtherBackend implements JsonBackend<JsonElement> {
        private final GsonBackend delegate = new GsonBackend();

        @Override
        public JsonElement newObject() {
            return delegate.newObject();
        }

        @Override
        public JsonElement newArray() {
            return delegate.newArray();
        }

        @Override
        public JsonElement of(String value) {
            return delegate.of(value);
        }

        @Override
        public JsonElement of(Number value) {
            return delegate.of(value);
        }

        @Override
        public JsonElement of(Boolean value) {
            return delegate.of(value);
        }

        @Override
        public void setProperty(JsonElement objectNode, String key, JsonElement value) {
            delegate.setProperty(objectNode, key, value);
        }

        @Override
        public void addElement(JsonElement arrayNode, JsonElement element) {
            delegate.addElement(arrayNode, element);
        }

        @Override
        public JsonElement parse(Reader in) throws IOException {
            return delegate.parse(in);
        }

        @Override
        public JsonValue.Kind kindOf(JsonElement node) {
            return delegate.kindOf(node);
        }

        @Override
        public String asString(JsonElement node) {
            return delegate.asString(node);
        }

        @Override
        public BigDecimal asDecimal(JsonElement node) {
            return delegate.asDecimal(node);
        }

        @Override
        public boolean asBoolean(JsonElement node) {
            return delegate.asBoolean(node);
        }

        @Override
        public JsonElement member(JsonElement objectNode, String key) {
            return delegate.member(objectNode, key);
        }

        @Override
        public Set<String> keys(JsonElement objectNode) {
            return delegate.keys(objectNode);
        }

        @Override
        public int size(JsonElement arrayNode) {
            return delegate.size(arrayNode);
        }

        @Override
        public JsonElement element(JsonElement arrayNode, int index) {
            return delegate.element(arrayNode, index);
        }

        @Override
        public String serialize(JsonElement node) {
            return delegate.serialize(node);
        }

        @Override
        public void write(JsonElement node, Writer out) throws IOException {
            delegate.write(node, out);
        }
    }

    /**
     * A key holds one value: the object-side Mapper overload takes a single object and assigns it as a scalar, while
     * the Iterable overload assigns an array. Passing several objects for one key used to compile and silently keep
     * only the last; it is now a compile error.
     */
    @Test
    public void testMapperSingleObjectVersusIterableOnObject() {
        Assert.assertEquals(Json.object().add("k", Mapper.STRING, "a").toJson(), "{\"k\":\"a\"}");
        Assert.assertEquals(
                Json.object().add("k", Mapper.STRING, List.of("a", "b", "c")).toJson(), "{\"k\":[\"a\",\"b\",\"c\"]}");
    }

    /**
     * On arrays both Mapper overloads remain, and they differ: varargs appends the mapped objects flat, the Iterable
     * overload nests them in a new array.
     */
    @Test
    public void testMapperVarargsVersusIterableOnArray() {
        Assert.assertEquals(Json.array().add(Mapper.STRING, "a", "b", "c").toJson(), "[\"a\",\"b\",\"c\"]");
        Assert.assertEquals(
                Json.array().add(Mapper.STRING, List.of("a", "b", "c")).toJson(), "[[\"a\",\"b\",\"c\"]]");
    }
}
