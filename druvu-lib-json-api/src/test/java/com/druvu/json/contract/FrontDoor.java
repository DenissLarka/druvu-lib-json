package com.druvu.json.contract;

import com.druvu.json.Json;
import com.druvu.json.JsonArrayBuilder;
import com.druvu.json.JsonBuilder;
import com.druvu.json.JsonObjectBuilder;
import com.druvu.json.JsonValue;
import com.druvu.json.Mapper;

/**
 * The entry points a contract run goes through: {@link Json} for a JSON backend, a format module's own facade
 * otherwise. Everything past the front door — builders, values, accessors, strictness — is shared, which is what lets
 * one contract cover every format.
 *
 * @author Deniss Larka
 */
public interface FrontDoor {

    /** {@link Json}'s entry points — the default for every contract. */
    FrontDoor JSON = new JsonFrontDoor();

    /** @return a builder for a new object. */
    JsonObjectBuilder<?> object();

    /** @return a builder for a new array. */
    JsonArrayBuilder<?> array();

    /**
     * @param transform the mapper.
     * @param objects the objects to map.
     * @param <T> the object type.
     * @return a builder for the array of mapped objects.
     */
    <T> JsonArrayBuilder<?> array(Mapper<T> transform, Iterable<T> objects);

    /**
     * @param value the value.
     * @return a standalone primitive builder.
     */
    JsonBuilder value(String value);

    /**
     * @param value the value.
     * @return a standalone primitive builder.
     */
    JsonBuilder value(Number value);

    /**
     * @param value the value.
     * @return a standalone primitive builder.
     */
    JsonBuilder value(Boolean value);

    /**
     * @param text a document in the format under test; contracts feed JSON text, which YAML 1.2 reads as well.
     * @return the parsed root value.
     */
    JsonValue parse(String text);
}
