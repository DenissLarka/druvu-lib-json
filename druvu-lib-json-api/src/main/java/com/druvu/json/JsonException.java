package com.druvu.json;

/**
 * Thrown when JSON cannot be parsed, or a {@link JsonValue} is asked for something it does not hold — a missing key, a
 * value of the wrong kind, a number that does not fit the requested type. The message carries the
 * {@linkplain JsonValue#path() path} of the offending value.
 */
public final class JsonException extends RuntimeException {

    public JsonException(String message) {
        super(message);
    }

    public JsonException(String message, Throwable cause) {
        super(message, cause);
    }
}
