package com.druvu.json;

/**
 * Transforms an object of your own type into JSON, for the {@code add} overloads that map objects while building. Being
 * a functional interface, a mapper is usually just a lambda:
 *
 * <pre>{@code
 * Mapper<Customer> customer = c -> Json.object().add("id", c.id()).add("name", c.name());
 * }</pre>
 *
 * <p>Mapping a plain value needs no more than the same lambda, {@code s -> Json.value(s)}. There are deliberately no
 * ready-made constants for that: a mapper builds through one front door, so a constant would silently bind every mapped
 * value to {@link Json}'s backend — including inside a {@link Yaml} document, where the result is refused as a mixed
 * backend.
 *
 * @param <T> the type being mapped.
 */
@FunctionalInterface
public interface Mapper<T> {

    /**
     * Transform the object into a builder.
     *
     * @param o the object to transform.
     * @return the builder.
     */
    JsonBuilder map(T o);
}
