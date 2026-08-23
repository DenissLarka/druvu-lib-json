package com.druvu.json;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.Set;

/**
 * A JSON object. Two access styles, one bargain each:
 *
 * <ul>
 *   <li>{@link #get(String)} and the typed shortcuts ({@link #string}, {@link #decimal}, …) are for keys the document
 *       must have — a missing key or a wrong kind throws {@link JsonException} naming the path.
 *   <li>{@link #find(String)} is for keys that may legitimately be absent — it returns {@link Optional#empty()} both
 *       for a missing key and for a key explicitly set to JSON {@code null}: on the read side a null value carries
 *       nothing a reader can use, so the two collapse and {@code find(key).map(JsonValue::asString)} is always safe.
 *       The raw fact stays reachable — {@link #has(String)} reports a null-valued key as present, and
 *       {@link #get(String)} returns it as a {@linkplain JsonValue#isNull() null value}.
 * </ul>
 */
public final class JsonObject extends BackedValue implements JsonValue {

    JsonObject(JsonBackend<Object> backend, Object node, String path) {
        super(backend, node, path);
    }

    @Override
    public Kind kind() {
        return Kind.OBJECT;
    }

    /**
     * @param key the member key.
     * @return the value under a key the document must have.
     * @throws JsonException if the key is absent; the message names the keys that are present.
     */
    public JsonValue get(String key) {
        Object member = backend.member(node, key);
        if (member == null) {
            throw new JsonException("Missing key '" + key + "' at " + path() + " (present: " + keys() + ")");
        }
        return wrap(backend, member, memberPath(key));
    }

    /**
     * @param key the member key.
     * @return the value under a key that may be absent: empty when the key is missing or its value is JSON {@code null}
     *     — a null on the wire reads as "not given".
     */
    public Optional<JsonValue> find(String key) {
        Object member = backend.member(node, key);
        if (member == null) {
            return Optional.empty();
        }
        JsonValue value = wrap(backend, member, memberPath(key));
        return value.isNull() ? Optional.empty() : Optional.of(value);
    }

    /**
     * @param key the member key.
     * @return whether the key is present (a key set to JSON {@code null} is present).
     */
    public boolean has(String key) {
        return backend.member(node, key) != null;
    }

    /** @return the member keys, in document order where the backend preserves it. */
    public Set<String> keys() {
        return backend.keys(node);
    }

    /** @return the number of members. */
    public int size() {
        return keys().size();
    }

    /**
     * @param key the member key.
     * @return the string under a required key — shorthand for {@code get(key).asString()}.
     */
    public String string(String key) {
        return get(key).asString();
    }

    /**
     * @param key the member key.
     * @return the number under a required key, exactly as it appeared on the wire.
     */
    public BigDecimal decimal(String key) {
        return get(key).asDecimal();
    }

    /**
     * @param key the member key.
     * @return the exact {@code int} under a required key.
     */
    public int intValue(String key) {
        return get(key).asInt();
    }

    /**
     * @param key the member key.
     * @return the exact {@code long} under a required key.
     */
    public long longValue(String key) {
        return get(key).asLong();
    }

    /**
     * @param key the member key.
     * @return the boolean under a required key.
     */
    public boolean bool(String key) {
        return get(key).asBoolean();
    }

    /**
     * @param key the member key.
     * @return the object under a required key.
     */
    public JsonObject object(String key) {
        return get(key).asObject();
    }

    /**
     * @param key the member key.
     * @return the array under a required key.
     */
    public JsonArray array(String key) {
        return get(key).asArray();
    }

    private String memberPath(String key) {
        return path() + "." + key;
    }
}
