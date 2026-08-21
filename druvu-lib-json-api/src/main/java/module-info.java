/**
 * Fluent JSON builder API with a pluggable serialization backend SPI.
 * <p>Backends implement {@code com.druvu.json.JsonBuilderFactory.Backend} and register
 * a {@code com.druvu.lib.loader.ComponentFactory} that creates them.
 */
module com.druvu.json {
	exports com.druvu.json;

	requires com.druvu.lib.loader;
}
