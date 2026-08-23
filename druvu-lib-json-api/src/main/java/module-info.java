/**
 * Fluent JSON building and strict parsing with a pluggable backend SPI.
 *
 * <p>Backends implement {@code com.druvu.json.JsonBackend} and register a {@code com.druvu.lib.loader.ComponentFactory}
 * that creates them.
 */
module com.druvu.json {
    exports com.druvu.json;

    requires com.druvu.lib.loader;
}
