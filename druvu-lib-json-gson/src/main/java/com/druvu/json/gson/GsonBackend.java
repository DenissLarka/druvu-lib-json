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

import com.druvu.json.JsonBackend;
import com.druvu.json.JsonValue;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSyntaxException;
import com.google.gson.Strictness;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;

/**
 * Gson backend: nodes are {@link JsonElement}s, so {@code raw()} returns the native Gson tree. Parsing is strict per
 * the SPI contract: RFC-8259 syntax only, no trailing content.
 *
 * @author Bryn Cooke
 * @author Deniss Larka
 */
public final class GsonBackend implements JsonBackend<JsonElement> {

    @Override
    public JsonElement newObject() {
        return new JsonObject();
    }

    @Override
    public JsonElement newArray() {
        return new JsonArray();
    }

    @Override
    public JsonElement nullNode() {
        return JsonNull.INSTANCE;
    }

    @Override
    public JsonElement of(String value) {
        return new JsonPrimitive(value);
    }

    @Override
    public JsonElement of(Number value) {
        return new JsonPrimitive(value);
    }

    @Override
    public JsonElement of(Boolean value) {
        return new JsonPrimitive(value);
    }

    @Override
    public void setProperty(JsonElement objectNode, String key, JsonElement value) {
        objectNode.getAsJsonObject().add(key, value);
    }

    @Override
    public void addElement(JsonElement arrayNode, JsonElement element) {
        arrayNode.getAsJsonArray().add(element);
    }

    @Override
    public JsonElement parse(Reader in) throws IOException {
        JsonReader reader = new JsonReader(in);
        reader.setStrictness(Strictness.STRICT);
        JsonElement element = JsonParser.parseReader(reader);
        if (reader.peek() != JsonToken.END_DOCUMENT) {
            throw new JsonSyntaxException("Trailing content after the JSON document");
        }
        return element;
    }

    @Override
    public JsonValue.Kind kindOf(JsonElement node) {
        return switch (node) {
            case JsonNull _ -> JsonValue.Kind.NULL;
            case JsonObject _ -> JsonValue.Kind.OBJECT;
            case JsonArray _ -> JsonValue.Kind.ARRAY;
            case JsonPrimitive primitive when primitive.isString() -> JsonValue.Kind.STRING;
            case JsonPrimitive primitive when primitive.isNumber() -> JsonValue.Kind.NUMBER;
            case JsonPrimitive _ -> JsonValue.Kind.BOOLEAN;
            default -> throw new IllegalArgumentException("Unknown node type: " + node.getClass());
        };
    }

    @Override
    public String asString(JsonElement node) {
        return node.getAsString();
    }

    @Override
    public BigDecimal asDecimal(JsonElement node) {
        return node.getAsBigDecimal();
    }

    @Override
    public boolean asBoolean(JsonElement node) {
        return node.getAsBoolean();
    }

    @Override
    public JsonElement member(JsonElement objectNode, String key) {
        return objectNode.getAsJsonObject().get(key);
    }

    @Override
    public Set<String> keys(JsonElement objectNode) {
        return objectNode.getAsJsonObject().keySet();
    }

    @Override
    public int size(JsonElement arrayNode) {
        return arrayNode.getAsJsonArray().size();
    }

    @Override
    public JsonElement element(JsonElement arrayNode, int index) {
        return arrayNode.getAsJsonArray().get(index);
    }

    @Override
    public String serialize(JsonElement node) {
        return node.toString();
    }

    @Override
    public void write(JsonElement node, Writer out) throws IOException {
        write(new JsonWriter(out), node);
    }

    /** Serialization code adapted from GSON */
    private void write(JsonWriter out, JsonElement value) throws IOException {
        switch (value == null ? JsonNull.INSTANCE : value) {
            case JsonNull _ -> out.nullValue();
            case JsonPrimitive primitive when primitive.isNumber() -> out.value(primitive.getAsNumber());
            case JsonPrimitive primitive when primitive.isBoolean() -> out.value(primitive.getAsBoolean());
            case JsonPrimitive primitive -> out.value(primitive.getAsString());
            case JsonArray array -> {
                out.beginArray();
                for (JsonElement element : array) {
                    write(out, element);
                }
                out.endArray();
            }
            case JsonObject object -> {
                out.beginObject();
                for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
                    out.name(entry.getKey());
                    write(out, entry.getValue());
                }
                out.endObject();
            }
            default -> throw new IllegalArgumentException("Couldn't write " + value.getClass());
        }
    }
}
