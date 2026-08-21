package com.druvu.json;

import java.time.temporal.Temporal;
import java.util.Date;


public interface JsonArrayBuilder<P, R> extends JsonBuilder {
	/**
	 * Add a new object as the next element in this array.
	 * @return the builder for the new object.
	 */
	JsonObjectBuilder<JsonArrayBuilder<P, R>, R> addObject();

	
	/**
	 * Add a new array as the next element in this array.
	 * @return the builder for the new array.
	 */
	JsonArrayBuilder<JsonArrayBuilder<P, R>, R> addArray();

	/**
     * End the current object and return to building the parent element.
     * @return the builder
     */
	P end();

	/**
	 * Add a single value to this array.
	 * 
	 * @param value the value to add.
	 * @return the current builder.
	 */
	JsonArrayBuilder<P, R> add(Boolean value);
	
	/**
	 * Add a single value to this array.
	 * 
	 * @param value the value to add.
	 * @return the current builder.
	 */
	JsonArrayBuilder<P, R> add(Character value);
	
	
	/**
	 * Add a single value to this array.
	 * 
	 * @param value the value to add.
	 * @return the current builder.
	 */
	JsonArrayBuilder<P, R> add(Number value);
	
	
	/**
	 * Add a single value to this array.
	 * 
	 * @param value the value to add.
	 * @return the current builder.
	 */
	JsonArrayBuilder<P, R> add(String value);
	/**
	 * Add a single value to this array.
	 * 
	 * @return the current builder.
	 */
	JsonArrayBuilder<P, R> addNull();
	
	/**
	 * Add a single value to this array.
	 * 
	 * @param value the value to add.
	 * @return the current builder.
	 */
	JsonArrayBuilder<P, R> add(Date value);
	
	/**
	 * Add a single value to this array.
	 * 
	 * @param value the value to add.
	 * @return the current builder.
	 */
	JsonArrayBuilder<P, R> add(Temporal value);
	
	/**
	 * Add an array of elements.
	 * 
	 * @param builders the builders to get the elements from.
	 * @return the current builder.
	 */
	JsonArrayBuilder<P, R> add(Iterable<? extends JsonBuilder> builders);
	
	/**
	 * Add a collection of elements to the current array.
	 * 
	 * @param builders the builders to get the elements from.
	 * @return the current builder.
	 */
	JsonArrayBuilder<P, R> addAll(Iterable<? extends JsonBuilder> builders);

	/**
	 * Add a single element.
	 * 
	 * @param builder the builder for the element to add.
	 * @return the current builder.
	 */
	JsonArrayBuilder<P, R> add(JsonBuilder builder);

	
	/**
	 * Add an array of elements.
	 * 
	 * @param transform The transformer for the object. 
	 * @param objects the objects to add.
	 * @param <T> The type of the objects
	 * @return the current builder.
	 */
	<T> JsonArrayBuilder<P, R> add(Mapper<T> transform, Iterable<T> objects);
	
	/**
	 * Add a collection of elements to the current array.
	 * 
	 * @param transform The transformer for the object.
	 * @param objects the objects to add.
	 * @param <T> The type of the objects
	 * @return the current builder.
	 */
	<T> JsonArrayBuilder<P, R> addAll(Mapper<T> transform, Iterable<T> objects);

	/**
	 * Add each mapped object as an element of this array, flat — unlike
	 * {@link #add(Mapper, Iterable)}, which nests them in a new array.
	 * 
	 * @param transform The transformer for the object.
	 * @param objects the objects to add.
	 * @param <T> The type of the objects
	 * @return the current builder.
	 */
	<T> JsonArrayBuilder<P, R> add(Mapper<T> transform, T... objects);

	
	/**
	 * @return the built JSON root node, in the backend's native type.
	 */
	R getJson();





	
}