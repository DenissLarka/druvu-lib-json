package com.druvu.json.contract;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.druvu.json.JsonException;
import org.testng.annotations.Test;

/**
 * JSON-text strictness: RFC 8259 syntax only. This part of the contract is specific to JSON as a text format — a YAML
 * backend legitimately accepts some of these inputs and does not run it.
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
}
