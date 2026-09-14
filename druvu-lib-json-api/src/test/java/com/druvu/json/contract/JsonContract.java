package com.druvu.json.contract;

import static org.assertj.core.api.Assertions.assertThat;

import com.druvu.json.Json;
import com.druvu.json.JsonArray;
import com.druvu.json.JsonArrayBuilder;
import com.druvu.json.JsonBackend;
import com.druvu.json.JsonBuilder;
import com.druvu.json.JsonObject;
import com.druvu.json.JsonObjectBuilder;
import com.druvu.json.JsonValue;
import com.druvu.json.Mapper;

/**
 * Root of the backend contract suite: what every backend must satisfy, expressed through the public API only.
 *
 * <p>A backend module runs the suite by subclassing each contract class once, naming its backend:
 *
 * <pre>{@code
 * public class TestGsonBuildContract extends JsonBuildContract {
 *     @Override
 *     protected JsonBackend<?> backend() {
 *         return new GsonBackend();
 *     }
 * }
 * }</pre>
 *
 * <p>Most tests go through the discovered front door ({@link Json}), so the module's own service registration is
 * exercised too. A format module with its own facade overrides {@link #frontDoor()} instead. The {@link #backend()}
 * hook serves the few tests that need an explicit backend, such as the backend-mixing guard.
 *
 * <p>Expected trees are compared structurally via {@link #assertSameTree}, never through serialized text: text shape is
 * a per-format concern and lives in each module's own tests.
 *
 * @author Deniss Larka
 */
public abstract class JsonContract {

    /** @return a fresh instance of the backend under test. */
    protected abstract JsonBackend<?> backend();

    /**
     * @return the entry points this run goes through. A format module with its own facade overrides this one method;
     *     everything the contract calls flows from it.
     */
    protected FrontDoor frontDoor() {
        return FrontDoor.JSON;
    }

    /** @return an object builder from the front door under test. */
    protected final JsonObjectBuilder<?> object() {
        return frontDoor().object();
    }

    /** @return an array builder from the front door under test. */
    protected final JsonArrayBuilder<?> array() {
        return frontDoor().array();
    }

    /**
     * @param transform the mapper.
     * @param objects the objects to map.
     * @param <T> the object type.
     * @return the mapped array, from the front door under test.
     */
    protected final <T> JsonArrayBuilder<?> array(Mapper<T> transform, Iterable<T> objects) {
        return frontDoor().array(transform, objects);
    }

    /**
     * @param value the value.
     * @return a standalone primitive builder from the front door under test.
     */
    protected final JsonBuilder value(String value) {
        return frontDoor().value(value);
    }

    /**
     * @param value the value.
     * @return a standalone primitive builder from the front door under test.
     */
    protected final JsonBuilder value(Number value) {
        return frontDoor().value(value);
    }

    /**
     * @param value the value.
     * @return a standalone primitive builder from the front door under test.
     */
    protected final JsonBuilder value(Boolean value) {
        return frontDoor().value(value);
    }

    /**
     * @param text a document in the format under test; the contract feeds JSON text, which YAML 1.2 reads as well.
     * @return the parsed root value.
     */
    protected final JsonValue parse(String text) {
        return frontDoor().parse(text);
    }

    /**
     * Asserts that two values carry the same JSON data: same kinds, member keys, elements and leaf values — whatever
     * the backend and whatever text it would serialize to.
     *
     * @param actual the value under test.
     * @param expected the expected value, usually {@link #parse(String) parsed} from literal JSON.
     */
    protected static void assertSameTree(JsonValue actual, JsonValue expected) {
        assertThat(actual.kind()).as("kind at %s", actual.path()).isEqualTo(expected.kind());
        switch (expected.kind()) {
            case OBJECT -> {
                JsonObject a = actual.asObject();
                JsonObject e = expected.asObject();
                assertThat(a.keys()).as("keys at %s", a.path()).containsExactlyInAnyOrderElementsOf(e.keys());
                for (String key : e.keys()) {
                    assertSameTree(a.get(key), e.get(key));
                }
            }
            case ARRAY -> {
                JsonArray a = actual.asArray();
                JsonArray e = expected.asArray();
                assertThat(a.size()).as("size at %s", a.path()).isEqualTo(e.size());
                for (int i = 0; i < e.size(); i++) {
                    assertSameTree(a.get(i), e.get(i));
                }
            }
            case STRING ->
                assertThat(actual.asString()).as("at %s", actual.path()).isEqualTo(expected.asString());
            case NUMBER ->
                assertThat(actual.asDecimal()).as("at %s", actual.path()).isEqualByComparingTo(expected.asDecimal());
            case BOOLEAN ->
                assertThat(actual.asBoolean()).as("at %s", actual.path()).isEqualTo(expected.asBoolean());
            case NULL -> {
                // kinds already match
            }
        }
    }
}
