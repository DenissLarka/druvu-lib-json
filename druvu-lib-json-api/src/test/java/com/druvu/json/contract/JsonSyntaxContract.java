package com.druvu.json.contract;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.druvu.json.Json;
import com.druvu.json.JsonException;
import com.druvu.json.JsonObject;
import com.druvu.json.JsonValue;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicReference;
import org.testng.annotations.Test;

/**
 * JSON-text strictness: RFC 8259 syntax only, and the template form, which is JSON text with placeholders. This part of
 * the contract is specific to JSON as a text format — a YAML backend legitimately accepts some of these inputs, and
 * {@code ?} means something in YAML, so it does not run it.
 *
 * @author Deniss Larka
 */
public abstract class JsonSyntaxContract extends JsonContract {

    @Test
    public void nonRfcSyntaxIsRefused() {
        assertThatThrownBy(() -> parse("{'single':1}")).isInstanceOf(JsonException.class);
        assertThatThrownBy(() -> parse("{unquoted:1}")).isInstanceOf(JsonException.class);
        assertThatThrownBy(() -> parse("{\"a\":}")).isInstanceOf(JsonException.class);
        assertThatThrownBy(() -> parse("[1,]")).isInstanceOf(JsonException.class);
        assertThatThrownBy(() -> parse("// comment\n{}")).isInstanceOf(JsonException.class);
    }

    @Test
    public void nonFiniteNumberTokensAreRefused() {
        assertThatThrownBy(() -> parse("NaN")).isInstanceOf(JsonException.class);
        assertThatThrownBy(() -> parse("Infinity")).isInstanceOf(JsonException.class);
        assertThatThrownBy(() -> parse("{\"n\":-Infinity}")).isInstanceOf(JsonException.class);
    }

    @Test
    public void aTemplateBindsPlaceholdersInOrderThroughTheBackend() {
        JsonObject filled = Json.fill(
                        """
                        {"name": ?, "amount": ?, "ok": ?, "day": ?, "nested": ?, "parsed": ?}
                        """,
                        backend(),
                        "a \"quoted\"\nline",
                        new BigDecimal("2.50"),
                        true,
                        LocalDate.of(2026, 10, 11),
                        object().add("x", 1),
                        parse("[1, 2]"))
                .asObject();
        assertThat(filled.string("name")).isEqualTo("a \"quoted\"\nline");
        assertThat(filled.decimal("amount")).isEqualTo(new BigDecimal("2.50"));
        assertThat(filled.bool("ok")).isTrue();
        assertThat(filled.string("day")).isEqualTo("2026-10-11");
        assertThat(filled.object("nested").intValue("x")).isEqualTo(1);
        assertThat(filled.array("parsed").stream().map(JsonValue::asInt)).containsExactly(1, 2);
    }

    @Test
    public void aTemplateLeavesQuestionMarksInsideStringsAlone() {
        JsonObject filled = Json.fill("{\"q\": \"really?\", \"e\": \"a \\\"?\\\" b\", \"v\": ?}", backend(), 1)
                .asObject();
        assertThat(filled.string("q")).isEqualTo("really?");
        assertThat(filled.string("e")).isEqualTo("a \"?\" b");
        assertThat(filled.intValue("v")).isEqualTo(1);
    }

    @Test
    public void aTemplateRefusesACountMismatch() {
        assertThatThrownBy(() -> Json.fill("{\"v\": ?}", backend(), 1, 2))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("2 values for 1 placeholders");
        assertThatThrownBy(() -> Json.fill("{\"a\": ?, \"b\": ?}", backend(), 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Placeholder 2 has no value");
    }

    @Test
    public void aTemplateRefusesNullAndForeignValues() {
        Object none = new AtomicReference<>().get();
        assertThatThrownBy(() -> Json.fill("{\"v\": ?}", backend(), none)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> Json.fill("{\"v\": ?}", backend(), new Object()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("java.lang.Object");
    }

    @Test
    public void aFilledTemplateIsParsedStrictly() {
        assertThatThrownBy(() -> Json.fill("{\"v\": ?", backend(), 1)).isInstanceOf(JsonException.class);
    }
}
