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

import java.io.IOException;
import java.io.Writer;
import java.util.Map;

import com.druvu.json.JsonBuilderFactory;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.stream.JsonWriter;

/**
 * Gson backend: nodes are {@link JsonElement}s, so {@code getJson()} returns the
 * native Gson tree.
 *
 * @author Bryn Cooke
 * @author Deniss Larka
 */
public final class GsonBackend implements JsonBuilderFactory.Backend<JsonElement> {

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
		((JsonObject) objectNode).add(key, value);
	}

	@Override
	public void addElement(JsonElement arrayNode, JsonElement element) {
		((JsonArray) arrayNode).add(element);
	}

	@Override
	public String serialize(JsonElement node) {
		return node.toString();
	}

	@Override
	public void write(JsonElement node, Writer out) throws IOException {
		write(new JsonWriter(out), node);
	}

	/**
	 * Serialization code copied from GSON
	 */
	private void write(JsonWriter out, JsonElement value) throws IOException {
		if (value == null || value.isJsonNull()) {
			out.nullValue();
		} else if (value.isJsonPrimitive()) {
			JsonPrimitive primitive = value.getAsJsonPrimitive();
			if (primitive.isNumber()) {
				out.value(primitive.getAsNumber());
			} else if (primitive.isBoolean()) {
				out.value(primitive.getAsBoolean());
			} else {
				out.value(primitive.getAsString());
			}

		} else if (value.isJsonArray()) {
			out.beginArray();
			for (JsonElement e : value.getAsJsonArray()) {
				write(out, e);
			}
			out.endArray();

		} else if (value.isJsonObject()) {
			out.beginObject();
			for (Map.Entry<String, JsonElement> e : value.getAsJsonObject().entrySet()) {
				out.name(e.getKey());
				write(out, e.getValue());
			}
			out.endObject();

		} else {
			throw new IllegalArgumentException("Couldn't write " + value.getClass());
		}
	}
}
