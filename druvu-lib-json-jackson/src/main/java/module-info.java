/** Jackson 3 backend for the druvu-lib-json building and parsing API. */
module com.druvu.json.jackson {
    exports com.druvu.json.jackson;

    requires transitive com.druvu.json;
    requires transitive tools.jackson.databind;
    requires com.druvu.lib.loader;

    provides com.druvu.lib.loader.ComponentFactory with
            com.druvu.json.jackson.JacksonBackendFactory;
}
