package com.druvu.json;

/**
 * A backend that reads and writes YAML rather than JSON.
 *
 * <p>The contract is {@link JsonBackend}'s, unchanged: YAML 1.2 is a superset of JSON, and its core schema carries the
 * same six value kinds, so the same node operations describe both. This interface adds no methods — it exists to give
 * YAML its own front door, {@link Yaml}. Discovery matches a factory by the exact class it declares, so a YAML backend
 * and a JSON backend can sit on one class path without either shadowing the other, while two backends of the same
 * format still fail loud.
 *
 * <p>Implementations register a {@code com.druvu.lib.loader.ComponentFactory} whose {@code type()} is
 * {@code YamlBackend.class}.
 *
 * @param <N> the engine's native node type.
 */
public interface YamlBackend<N> extends JsonBackend<N> {}
