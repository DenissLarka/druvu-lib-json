/** YAML 1.2 backend for the druvu-lib-json building and parsing API, on snakeyaml-engine. */
module com.druvu.json.yaml {
    exports com.druvu.json.yaml;

    requires transitive com.druvu.json;
    requires transitive org.snakeyaml.engine;
    requires com.druvu.lib.loader;

    provides com.druvu.lib.loader.ComponentFactory with
            com.druvu.json.yaml.SnakeYamlBackendFactory;
}
