package com.druvu.json;

import java.math.BigDecimal;
import java.util.Objects;
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
 *
 * <p>An optional member has two typed shapes on top of {@code find}: {@link #findString} and its siblings return an
 * {@link Optional}, and {@link #string(String, String)} and its siblings answer a fallback instead. Both stay strict on
 * kind: a present value of another type throws, as everywhere else.
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

    /**
     * @param key the member key.
     * @param fallback the answer when the key is missing or its value is JSON {@code null}.
     * @return the string under the key, or the fallback; a value of another kind throws.
     */
    public String string(String key, String fallback) {
        return findString(key).orElse(Objects.requireNonNull(fallback, "fallback"));
    }

    /**
     * @param key the member key.
     * @param fallback the answer when the key is missing or its value is JSON {@code null}.
     * @return the number under the key, or the fallback; a value of another kind throws.
     */
    public BigDecimal decimal(String key, BigDecimal fallback) {
        return findDecimal(key).orElse(Objects.requireNonNull(fallback, "fallback"));
    }

    /**
     * @param key the member key.
     * @param fallback the answer when the key is missing or its value is JSON {@code null}.
     * @return the exact {@code int} under the key, or the fallback; a value of another kind throws.
     */
    public int intValue(String key, int fallback) {
        return findInt(key).orElse(fallback);
    }

    /**
     * @param key the member key.
     * @param fallback the answer when the key is missing or its value is JSON {@code null}.
     * @return the exact {@code long} under the key, or the fallback; a value of another kind throws.
     */
    public long longValue(String key, long fallback) {
        return findLong(key).orElse(fallback);
    }

    /**
     * @param key the member key.
     * @param fallback the answer when the key is missing or its value is JSON {@code null}.
     * @return the boolean under the key, or the fallback; a value of another kind throws.
     */
    public boolean bool(String key, boolean fallback) {
        return findBool(key).orElse(fallback);
    }

    /**
     * @param key the member key.
     * @return the string under a key that may be absent: empty for a missing or null-valued key; a value of another
     *     kind throws.
     */
    public Optional<String> findString(String key) {
        return find(key).map(JsonValue::asString);
    }

    /**
     * @param key the member key.
     * @return the number under a key that may be absent: empty for a missing or null-valued key; a value of another
     *     kind throws.
     */
    public Optional<BigDecimal> findDecimal(String key) {
        return find(key).map(JsonValue::asDecimal);
    }

    /**
     * @param key the member key.
     * @return the exact {@code int} under a key that may be absent: empty for a missing or null-valued key; a value of
     *     another kind, or a number that is not an exact {@code int}, throws.
     */
    public Optional<Integer> findInt(String key) {
        return find(key).map(JsonValue::asInt);
    }

    /**
     * @param key the member key.
     * @return the exact {@code long} under a key that may be absent: empty for a missing or null-valued key; a value of
     *     another kind, or a number that is not an exact {@code long}, throws.
     */
    public Optional<Long> findLong(String key) {
        return find(key).map(JsonValue::asLong);
    }

    /**
     * @param key the member key.
     * @return the boolean under a key that may be absent: empty for a missing or null-valued key; a value of another
     *     kind throws.
     */
    public Optional<Boolean> findBool(String key) {
        return find(key).map(JsonValue::asBoolean);
    }

    /**
     * @param key the member key.
     * @return the object under a key that may be absent: empty for a missing or null-valued key; a value of another
     *     kind throws.
     */
    public Optional<JsonObject> findObject(String key) {
        return find(key).map(JsonValue::asObject);
    }

    /**
     * @param key the member key.
     * @return the array under a key that may be absent: empty for a missing or null-valued key; a value of another kind
     *     throws.
     */
    public Optional<JsonArray> findArray(String key) {
        return find(key).map(JsonValue::asArray);
    }

    private String memberPath(String key) {
        return path() + "." + key;
    }
}
