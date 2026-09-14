package com.druvu.json.jackson;

import com.druvu.json.JsonBackend;
import com.druvu.json.JsonValue;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.LinkedHashSet;
import java.util.Set;
import tools.jackson.core.StreamWriteFeature;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.cfg.JsonNodeFeature;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.DecimalNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

/**
 * Jackson 3 backend: nodes are {@link JsonNode}s, so {@code raw()} returns Jackson's native tree. Parsing is strict per
 * the SPI contract — Jackson's defaults already refuse comments, single quotes, unquoted names and non-finite number
 * tokens; the mapper below adds the rest: one document only, and decimals read exactly as written.
 *
 * @author Deniss Larka
 */
public final class JacksonBackend implements JsonBackend<JsonNode> {

    private static final JsonNodeFactory NODES = JsonNodeFactory.instance;

    private static final JsonMapper MAPPER = JsonMapper.builder()
            // Numbers never pass through a double: 0.10 stays 0.10, 1e400 stays finite.
            .enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
            .enable(JsonNodeFeature.USE_BIG_DECIMAL_FOR_FLOATS)
            .disable(JsonNodeFeature.STRIP_TRAILING_BIGDECIMAL_ZEROES)
            // A document is one value; anything after it is an error, not ignored.
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
            // The caller owns the Writer handed to write(); Jackson must not close it.
            .disable(StreamWriteFeature.AUTO_CLOSE_TARGET)
            .build();

    @Override
    public JsonNode newObject() {
        return NODES.objectNode();
    }

    @Override
    public JsonNode newArray() {
        return NODES.arrayNode();
    }

    @Override
    public JsonNode of(String value) {
        return NODES.stringNode(value);
    }

    @Override
    public JsonNode of(Number value) {
        return switch (value) {
            case Integer i -> NODES.numberNode(i);
            case Long l -> NODES.numberNode(l);
            case BigDecimal d -> DecimalNode.valueOf(d);
            case BigInteger b -> NODES.numberNode(b);
            case Double d -> NODES.numberNode(d);
            case Float f -> NODES.numberNode(f);
            case Short s -> NODES.numberNode(s);
            case Byte b -> NODES.numberNode(b);
            // Any other Number (atomics, lazily parsed numbers, ...) goes through its decimal text, exactly.
            default -> DecimalNode.valueOf(new BigDecimal(value.toString()));
        };
    }

    @Override
    public JsonNode of(Boolean value) {
        return NODES.booleanNode(value);
    }

    @Override
    public void setProperty(JsonNode objectNode, String key, JsonNode value) {
        if (objectNode instanceof ObjectNode object) {
            object.set(key, value);
        } else {
            throw new IllegalArgumentException("Not an object node: " + objectNode.getNodeType());
        }
    }

    @Override
    public void addElement(JsonNode arrayNode, JsonNode element) {
        if (arrayNode instanceof ArrayNode array) {
            array.add(element);
        } else {
            throw new IllegalArgumentException("Not an array node: " + arrayNode.getNodeType());
        }
    }

    @Override
    public JsonNode parse(Reader in) throws IOException {
        JsonNode node = MAPPER.readTree(in);
        if (node == null || node.isMissingNode()) {
            // Jackson answers an empty document with a MissingNode; an empty document is not a document.
            throw new IllegalArgumentException("Empty document: expected a JSON value");
        }
        return node;
    }

    @Override
    public JsonValue.Kind kindOf(JsonNode node) {
        return switch (node.getNodeType()) {
            case OBJECT -> JsonValue.Kind.OBJECT;
            case ARRAY -> JsonValue.Kind.ARRAY;
            case STRING -> JsonValue.Kind.STRING;
            case NUMBER -> JsonValue.Kind.NUMBER;
            case BOOLEAN -> JsonValue.Kind.BOOLEAN;
            case NULL -> JsonValue.Kind.NULL;
            case BINARY, POJO, MISSING -> throw new IllegalArgumentException("Not a JSON node: " + node.getNodeType());
        };
    }

    @Override
    public String asString(JsonNode node) {
        return node.stringValue();
    }

    @Override
    public BigDecimal asDecimal(JsonNode node) {
        return node.decimalValue();
    }

    @Override
    public boolean asBoolean(JsonNode node) {
        return node.booleanValue();
    }

    @Override
    public JsonNode member(JsonNode objectNode, String key) {
        return objectNode.get(key);
    }

    @Override
    public Set<String> keys(JsonNode objectNode) {
        return new LinkedHashSet<>(objectNode.propertyNames());
    }

    @Override
    public int size(JsonNode arrayNode) {
        return arrayNode.size();
    }

    @Override
    public JsonNode element(JsonNode arrayNode, int index) {
        return arrayNode.get(index);
    }

    @Override
    public String serialize(JsonNode node) {
        return MAPPER.writeValueAsString(node);
    }

    @Override
    public void write(JsonNode node, Writer out) throws IOException {
        MAPPER.writeValue(out, node);
    }
}
