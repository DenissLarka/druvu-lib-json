package com.druvu.json;

import java.util.Iterator;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/** A JSON array. Iterable, streamable, and index-accessible; out-of-bounds access throws {@link JsonException}. */
public final class JsonArray extends BackedValue implements JsonValue, Iterable<JsonValue> {

    JsonArray(JsonBackend<Object> backend, Object node, String path) {
        super(backend, node, path);
    }

    @Override
    public Kind kind() {
        return Kind.ARRAY;
    }

    /** @return the number of elements. */
    public int size() {
        return backend.size(node);
    }

    /**
     * @param index the element index.
     * @return the element at an index.
     * @throws JsonException if the index is out of bounds.
     */
    public JsonValue get(int index) {
        int size = size();
        if (index < 0 || index >= size) {
            throw new JsonException("Index " + index + " out of bounds (size " + size + ") at " + path());
        }
        return wrap(backend, backend.element(node, index), path() + "[" + index + "]");
    }

    /** @return the elements as a stream, in order. */
    public Stream<JsonValue> stream() {
        return IntStream.range(0, size()).mapToObj(this::get);
    }

    @Override
    public Iterator<JsonValue> iterator() {
        return stream().iterator();
    }
}
