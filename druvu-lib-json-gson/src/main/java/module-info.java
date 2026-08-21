/**
 * Gson backend for the fluent-json builder API.
 */
module com.druvu.json.gson {
	exports com.druvu.json.gson;

	requires transitive com.druvu.json;
	requires transitive com.google.gson;
	requires com.druvu.lib.loader;

	provides com.druvu.lib.loader.ComponentFactory with com.druvu.json.gson.GsonBackendFactory;
}
