package com.druvu.json.contract;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.druvu.json.JsonArray;
import com.druvu.json.JsonException;
import com.druvu.json.JsonObject;
import com.druvu.json.JsonPrimitive;
import com.druvu.json.JsonValue;
import java.math.BigDecimal;
import java.util.List;
import org.testng.annotations.Test;

/**
 * The reading side of the contract: a document becomes a navigable tree with strict, fail-loud accessors, exact
 * numbers, the null doctrine on read, and diagnostics that name the path. Inputs are JSON text, which every format
 * under contract reads.
 *
 * @author Deniss Larka
 */
public abstract class JsonParseContract extends JsonContract {

    /** A wire-shaped payload: nested objects, an array, an exact decimal, an explicit null. */
    protected static final String PAYLOAD = """
            {
              "customers": [
                {
                  "id": "C-1001",
                  "name": "Alice",
                  "active": true,
                  "balance": 123456789.123456789012345678,
                  "sequence": 42,
                  "closedAt": null,
                  "tags": ["private", "chf"]
                }
              ],
              "total": 1
            }""";

    @Test
    public void navigatesWithTypedAccessors() {
        JsonObject root = parse(PAYLOAD).asObject();

        assertThat(root.longValue("total")).isEqualTo(1L);
        JsonArray customers = root.array("customers");
        assertThat(customers.size()).isEqualTo(1);

        JsonObject customer = customers.get(0).asObject();
        assertThat(customer.string("id")).isEqualTo("C-1001");
        assertThat(customer.bool("active")).isTrue();
        assertThat(customer.intValue("sequence")).isEqualTo(42);
        assertThat(customer.array("tags").stream().map(JsonValue::asString)).containsExactly("private", "chf");
    }

    @Test
    public void numbersStayExactBeyondDoublePrecision() {
        assertThat(firstCustomer().decimal("balance")).isEqualTo(new BigDecimal("123456789.123456789012345678"));
    }

    @Test
    public void numbersKeepTheirWireForm() {
        assertThat(parse("0.10").asDecimal()).isEqualTo(new BigDecimal("0.10"));
        assertThat(parse("1e400").asDecimal()).isEqualByComparingTo(new BigDecimal("1e400"));
        assertThat(parse("-0").asDecimal()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    public void nullValuedKeyReadsAsNotGiven() {
        JsonObject customer = firstCustomer();

        assertThat(customer.find("missing")).isEmpty();
        assertThat(customer.find("closedAt")).isEmpty();

        // The raw facts stay reachable for the rare caller who needs them.
        assertThat(customer.has("closedAt")).isTrue();
        assertThat(customer.has("missing")).isFalse();
        assertThat(customer.get("closedAt").isNull()).isTrue();
    }

    @Test
    public void findComposesForTolerantReads() {
        JsonObject customer = firstCustomer();

        assertThat(customer.find("name").map(JsonValue::asString)).contains("Alice");
        assertThat(customer.find("closedAt").map(JsonValue::asString)).isEmpty();
    }

    @Test
    public void missingKeyNamesPathAndPresentKeys() {
        assertThatThrownBy(() -> firstCustomer().get("nope"))
                .isInstanceOf(JsonException.class)
                .hasMessageContaining("Missing key 'nope'")
                .hasMessageContaining("$.customers[0]")
                .hasMessageContaining("id");
    }

    @Test
    public void wrongKindNamesExpectedActualAndPath() {
        assertThatThrownBy(() -> firstCustomer().get("name").asDecimal())
                .isInstanceOf(JsonException.class)
                .hasMessageContaining("Expected NUMBER")
                .hasMessageContaining("found STRING")
                .hasMessageContaining("$.customers[0].name");
    }

    @Test
    public void indexOutOfBoundsNamesPath() {
        JsonArray customers = parse(PAYLOAD).asObject().array("customers");
        assertThatThrownBy(() -> customers.get(5))
                .isInstanceOf(JsonException.class)
                .hasMessageContaining("out of bounds")
                .hasMessageContaining("$.customers");
    }

    @Test
    public void integerConversionsAreExact() {
        assertThat(parse("42").asInt()).isEqualTo(42);
        assertThat(parse("9999999999").asLong()).isEqualTo(9_999_999_999L);

        assertThatThrownBy(() -> parse("2.5").asInt()).isInstanceOf(JsonException.class);
        assertThatThrownBy(() -> parse("9999999999").asInt()).isInstanceOf(JsonException.class);
    }

    @Test
    public void topLevelPrimitivesParse() {
        assertThat(parse("\"x\"").asString()).isEqualTo("x");
        assertThat(parse("true").asBoolean()).isTrue();
        assertThat(parse("null").isNull()).isTrue();
    }

    /** Gson, Jackson and JavaScript all keep the last one, and reading is the tolerant side of the house rule. */
    @Test
    public void duplicateKeysResolveToTheLastValue() {
        JsonObject o = parse("{\"a\":1,\"a\":2}").asObject();
        assertThat(o.intValue("a")).isEqualTo(2);
        assertThat(o.size()).isEqualTo(1);
    }

    @Test
    public void malformedInputIsRefused() {
        assertThatThrownBy(() -> parse("{bad json")).isInstanceOf(JsonException.class);
        assertThatThrownBy(() -> parse("{\"a\":1")).isInstanceOf(JsonException.class);
    }

    @Test
    public void emptyDocumentIsRefused() {
        assertThatThrownBy(() -> parse("")).isInstanceOf(JsonException.class);
        assertThatThrownBy(() -> parse(" \n\t")).isInstanceOf(JsonException.class);
    }

    @Test
    public void trailingContentIsRefused() {
        assertThatThrownBy(() -> parse("{} garbage")).isInstanceOf(JsonException.class);
        assertThatThrownBy(() -> parse("[1][2]")).isInstanceOf(JsonException.class);
    }

    @Test
    public void buildResultNavigatesWithoutSerialization() {
        JsonObject built = object().add("a", "b")
                .addArray("nums")
                .add(1)
                .add(2)
                .end()
                .build()
                .asObject();

        assertThat(built.string("a")).isEqualTo("b");
        assertThat(built.array("nums").get(1).asInt()).isEqualTo(2);
    }

    @Test
    public void roundTripSurvivesSerialization() {
        JsonValue built =
                object().add("a", "b").add("n", new BigDecimal("2.50")).build();
        JsonValue reparsed = parse(built.toJson());

        assertSameTree(reparsed, built);
    }

    @Test
    public void parsedValueGraftsIntoBuilder() {
        JsonValue fragment = parse("{\"isin\":\"CH0012345678\"}");
        JsonValue built = object().add("instrument", fragment).build();
        assertSameTree(built, parse("{\"instrument\":{\"isin\":\"CH0012345678\"}}"));
    }

    @Test
    public void sealedHierarchySupportsPatternMatching() {
        String described =
                switch (parse(PAYLOAD)) {
                    case JsonObject o -> "object with " + o.size() + " members";
                    case JsonArray a -> "array of " + a.size();
                    case JsonPrimitive p -> "primitive " + p.kind();
                };
        assertThat(described).isEqualTo("object with 2 members");
    }

    @Test
    public void keysKeepDocumentOrder() {
        JsonObject o = parse("{\"z\":1,\"a\":2,\"m\":3}").asObject();
        assertThat(List.copyOf(o.keys())).containsExactly("z", "a", "m");
    }

    @Test
    public void arraysIterate() {
        int sum = 0;
        for (JsonValue v : parse("[1,2,3]").asArray()) {
            sum += v.asInt();
        }
        assertThat(sum).isEqualTo(6);
    }

    private JsonObject firstCustomer() {
        return parse(PAYLOAD).asObject().array("customers").get(0).asObject();
    }
}
