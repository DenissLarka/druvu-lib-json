package com.druvu.json;

import java.time.temporal.Temporal;
import java.util.Date;

public interface JsonObjectBuilder<P, R> extends JsonBuilder {
	/**
	 * Add a new object to this object using the specified key.
	 *
	 * @param key The key.
	 * @return The builder for the new object.
	 */
	JsonObjectBuilder<JsonObjectBuilder<P, R>, R> addObject(String key);

	/**
	 * Add a new array to this object using the specified key.
	 *
	 * @param key The key.
	 * @return A builder for the new object.
	 */
	JsonArrayBuilder<JsonObjectBuilder<P, R>, R> addArray(String key);

	/**
	 * End the current object and return to building the parent element.
	 *
	 * @return the builder
	 */
	P end();

	/**
	 * Add an array of elements assigned to a key.
	 *
	 * @param key      The key for the new element.
	 * @param builders The builders for the elements.
	 * @return The current builder.
	 */
	JsonObjectBuilder<P, R> add(String key, Iterable<? extends JsonBuilder> builders);

	/**
	 * Add an array of elements assigned to a key.
	 *
	 * @param key       The key for the new element.
	 * @param transform The transformer for the object.
	 * @param objects   The objects to add.
	 * @param <T>       The type of the objects
	 * @return The current builder.
	 */
	<T> JsonObjectBuilder<P, R> add(String key, Mapper<T> transform, Iterable<T> objects);

	/**
	 * Add a single element assigned to a key.
	 *
	 * @param key     The key for the new element.
	 * @param builder The builder for the element.
	 * @return The current builder.
	 */
	JsonObjectBuilder<P, R> add(String key, JsonBuilder builder);

	/**
	 * Add a single mapped object assigned to a key.
	 * <p>To assign several objects as an array, use
	 * {@link #add(String, Mapper, Iterable)} — a key holds one value, so this
	 * method takes one object.
	 *
	 * @param key       The key for the new element.
	 * @param transform The transformer for the object.
	 * @param object    The object to add.
	 * @param <T>       The objects type.
	 * @return The current builder.
	 */
	<T> JsonObjectBuilder<P, R> add(String key, Mapper<T> transform, T object);

	/**
	 * Add a simple property
	 *
	 * @param key   The key for the property.
	 * @param value the value to assign
	 * @return the current builder.
	 */
	JsonObjectBuilder<P, R> add(String key, Boolean value);

	/**
	 * Add a simple property
	 *
	 * @param key   The key for the property.
	 * @param value the value to assign
	 * @return the current builder.
	 */
	JsonObjectBuilder<P, R> add(String key, Character value);

	/**
	 * Add a simple property
	 *
	 * @param key   The key for the property.
	 * @param value the value to assign
	 * @return the current builder.
	 */
	JsonObjectBuilder<P, R> add(String key, Number value);

	/**
	 * Add a null property. Note that other add methods also accept null.
	 *
	 * @param key The key for the property.
	 * @return the current builder.
	 */
	JsonObjectBuilder<P, R> addNull(String key);

	/**
	 * Add a simple property
	 *
	 * @param key   The key for the property.
	 * @param value the value to assign
	 * @return the current builder.
	 */
	JsonObjectBuilder<P, R> add(String key, String value);

	/**
	 * Add a simple property
	 *
	 * @param key   The key for the property.
	 * @param value the value to assign
	 * @return the current builder.
	 */
	JsonObjectBuilder<P, R> add(String key, Temporal value);

	/**
	 * Add a simple property
	 *
	 * @param key   The key for the property.
	 * @param value the value to assign
	 * @return the current builder.
	 */
	JsonObjectBuilder<P, R> add(String key, Date value);

	/**
	 * @return the built JSON root node, in the backend's native type.
	 */
	R getJson();
}