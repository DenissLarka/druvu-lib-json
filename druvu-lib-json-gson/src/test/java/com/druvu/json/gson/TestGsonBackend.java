package com.druvu.json.gson;

import static org.assertj.core.api.Assertions.assertThat;

import com.druvu.json.Json;
import com.druvu.json.JsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import org.testng.annotations.Test;

/**
 * What is Gson's alone: discovery lands on this backend, {@code raw()} hands out Gson's tree, and the JSON text it
 * emits is compact RFC 8259. Everything backend-neutral lives in the contract suite.
 *
 * @author Deniss Larka
 */
public class TestGsonBackend {

    @Test
    public void discoveredBackendIsGson() {
        assertThat(Json.object().build().raw()).isInstanceOf(JsonElement.class);
        assertThat(Json.parse("[]").raw()).isInstanceOf(JsonElement.class);
    }

    @Test
    public void rawIsGsonsOwnTree() {
        Object raw = Json.object(new GsonBackend())
                .add("name", "Alice")
                .add("age", 30)
                .build()
                .raw();
        assertThat(raw).isEqualTo(JsonParser.parseString("{\"name\":\"Alice\",\"age\":30}"));
    }

    @Test
    public void serializesCompactJsonText() throws IOException {
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
                .add("Prop6", Instant.ofEpochMilli(0))
                .add("Prop7", LocalDate.of(0, 1, 1));

        StringWriter writer = new StringWriter();
        builder.write(writer);
        String expected =
                "{\"Prop1\":\"1\",\"Prop2\":2,\"Prop5\":{\"NP1\":4},\"Foo\":[{},\"AE1\"],\"Prop6\":\"1970-01-01T00:00:00Z\",\"Prop7\":\"0000-01-01\"}";
        assertThat(builder.toJson()).isEqualTo(expected);
        assertThat(builder.toString()).isEqualTo(expected);
        assertThat(writer.toString()).isEqualTo(expected);
    }

    @Test
    public void primitivesSerializeAsJsonText() {
        assertThat(Json.value(42).toJson()).isEqualTo("42");
        assertThat(Json.value("x").toJson()).isEqualTo("\"x\"");
        assertThat(Json.value(false).toJson()).isEqualTo("false");
    }

    /** Gson's nodes compare structurally, so values over them do too. That is Gson's doing, not a promise of ours. */
    @Test
    public void valuesCompareStructurally() {
        assertThat(Json.parse("{\"a\":1}")).isEqualTo(Json.parse("{\"a\":1}")).isNotEqualTo(Json.parse("{\"a\":2}"));
    }

    @Test
    public void bigDecimalTextIsPlain() {
        String json = Json.object()
                .add("v", new BigDecimal("0.1000000000000000055511151231257827"))
                .toJson();
        assertThat(json).isEqualTo("{\"v\":0.1000000000000000055511151231257827}");
    }
}
