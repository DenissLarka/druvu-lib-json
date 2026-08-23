package com.druvu.json.gson;

import static org.assertj.core.api.Assertions.assertThat;

import com.druvu.json.Json;
import com.druvu.json.JsonArray;
import com.druvu.json.JsonException;
import com.druvu.json.JsonObject;
import com.druvu.json.JsonPrimitive;
import com.druvu.json.JsonValue;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * The reading side: strict parsing into a navigable {@link JsonValue}, typed fail-loud accessors, and null-valued keys
 * reading as not given.
 *
 * @author Deniss Larka
 */
public class TestJsonParse {

    /** A wire-shaped payload: nested objects, an array, an exact decimal, an explicit null. */
    private static final String PAYLOAD = """
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
        JsonObject root = Json.parse(PAYLOAD).asObject();

        Assert.assertEquals(root.longValue("total"), 1L);
        JsonArray customers = root.array("customers");
        Assert.assertEquals(customers.size(), 1);

        JsonObject customer = customers.get(0).asObject();
        Assert.assertEquals(customer.string("id"), "C-1001");
        Assert.assertTrue(customer.bool("active"));
        Assert.assertEquals(customer.intValue("sequence"), 42);

        List<String> tags =
                customer.array("tags").stream().map(JsonValue::asString).collect(Collectors.toList());
        Assert.assertEquals(tags, List.of("private", "chf"));
    }

    @Test
    public void numbersStayExactBeyondDoublePrecision() {
        JsonObject customer = firstCustomer();
        Assert.assertEquals(customer.decimal("balance"), new BigDecimal("123456789.123456789012345678"));
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
        JsonException e =
                Assert.expectThrows(JsonException.class, () -> firstCustomer().get("nope"));
        Assert.assertTrue(e.getMessage().contains("Missing key 'nope'"), e.getMessage());
        Assert.assertTrue(e.getMessage().contains("$.customers[0]"), e.getMessage());
        Assert.assertTrue(e.getMessage().contains("id"), e.getMessage());
    }

    @Test
    public void wrongKindNamesExpectedActualAndPath() {
        JsonException e = Assert.expectThrows(
                JsonException.class, () -> firstCustomer().get("name").asDecimal());
        Assert.assertTrue(e.getMessage().contains("Expected NUMBER"), e.getMessage());
        Assert.assertTrue(e.getMessage().contains("found STRING"), e.getMessage());
        Assert.assertTrue(e.getMessage().contains("$.customers[0].name"), e.getMessage());
    }

    @Test
    public void indexOutOfBoundsNamesPath() {
        JsonArray customers = Json.parse(PAYLOAD).asObject().array("customers");
        JsonException e = Assert.expectThrows(JsonException.class, () -> customers.get(5));
        Assert.assertTrue(e.getMessage().contains("out of bounds"), e.getMessage());
        Assert.assertTrue(e.getMessage().contains("$.customers"), e.getMessage());
    }

    @Test
    public void integerConversionsAreExact() {
        Assert.assertEquals(Json.parse("42").asInt(), 42);
        Assert.assertEquals(Json.parse("9999999999").asLong(), 9_999_999_999L);

        Assert.expectThrows(JsonException.class, () -> Json.parse("2.5").asInt());
        Assert.expectThrows(JsonException.class, () -> Json.parse("9999999999").asInt());
    }

    @Test
    public void topLevelPrimitivesParse() {
        Assert.assertEquals(Json.parse("\"x\"").asString(), "x");
        Assert.assertTrue(Json.parse("true").asBoolean());
        Assert.assertTrue(Json.parse("null").isNull());
    }

    @Test
    public void malformedInputIsRefused() {
        Assert.expectThrows(JsonException.class, () -> Json.parse("{bad json"));
        Assert.expectThrows(JsonException.class, () -> Json.parse("{\"a\":}"));
    }

    @Test
    public void trailingContentIsRefused() {
        Assert.expectThrows(JsonException.class, () -> Json.parse("{} garbage"));
        Assert.expectThrows(JsonException.class, () -> Json.parse("[1][2]"));
    }

    @Test
    public void nonRfcSyntaxIsRefused() {
        Assert.expectThrows(JsonException.class, () -> Json.parse("{'single':1}"));
        Assert.expectThrows(JsonException.class, () -> Json.parse("{unquoted:1}"));
    }

    @Test
    public void buildResultNavigatesWithoutSerialization() {
        JsonObject built = Json.object()
                .add("a", "b")
                .addArray("nums")
                .add(1)
                .add(2)
                .end()
                .build()
                .asObject();

        Assert.assertEquals(built.string("a"), "b");
        Assert.assertEquals(built.array("nums").get(1).asInt(), 2);
    }

    @Test
    public void roundTripSurvivesSerialization() {
        JsonValue built =
                Json.object().add("a", "b").add("n", new BigDecimal("2.50")).build();
        JsonValue reparsed = Json.parse(built.toJson());
        Assert.assertEquals(reparsed, built);
    }

    @Test
    public void parsedValueGraftsIntoBuilder() {
        JsonValue fragment = Json.parse("{\"isin\":\"CH0012345678\"}");
        String out = Json.object().add("instrument", fragment).toJson();
        Assert.assertEquals(out, "{\"instrument\":{\"isin\":\"CH0012345678\"}}");
    }

    @Test
    public void sealedHierarchySupportsPatternMatching() {
        String described =
                switch (Json.parse(PAYLOAD)) {
                    case JsonObject o -> "object with " + o.size() + " members";
                    case JsonArray a -> "array of " + a.size();
                    case JsonPrimitive p -> "primitive " + p.kind();
                };
        Assert.assertEquals(described, "object with 2 members");
    }

    @Test
    public void keysKeepDocumentOrder() {
        JsonObject o = Json.parse("{\"z\":1,\"a\":2,\"m\":3}").asObject();
        Assert.assertEquals(List.copyOf(o.keys()), List.of("z", "a", "m"));
    }

    @Test
    public void arraysIterate() {
        int sum = 0;
        for (JsonValue v : Json.parse("[1,2,3]").asArray()) {
            sum += v.asInt();
        }
        Assert.assertEquals(sum, 6);
    }

    private static JsonObject firstCustomer() {
        return Json.parse(PAYLOAD).asObject().array("customers").get(0).asObject();
    }
}
