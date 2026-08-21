package com.druvu.json;

public interface Mapper<T> {

    /**
     * Transform the object in to a builder.
     *
     * @param o the object to transform.
     * @return The builder.
     */
    JsonBuilder map(T o);

    Mapper<String> STRING = JsonBuilderFactory::buildPrimitive;

    Mapper<Number> NUMBER = JsonBuilderFactory::buildPrimitive;

    Mapper<Boolean> BOOLEAN = JsonBuilderFactory::buildPrimitive;

    Mapper<Character> CHARACTER = JsonBuilderFactory::buildPrimitive;
}
