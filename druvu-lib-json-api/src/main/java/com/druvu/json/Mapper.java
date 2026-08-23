package com.druvu.json;

/**
 * Transforms an object of your own type into JSON, for the {@code add} overloads that map objects while building. Being
 * a functional interface, a mapper is usually just a lambda:
 *
 * <pre>{@code
 * Mapper<Customer> customer = c -> Json.object().add("id", c.id()).add("name", c.name());
 * }</pre>
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

    Mapper<String> STRING = Json::value;

    Mapper<Number> NUMBER = Json::value;

    Mapper<Boolean> BOOLEAN = Json::value;
}
