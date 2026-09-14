package com.druvu.json.yaml;

import com.druvu.json.JsonArrayBuilder;
import com.druvu.json.JsonBuilder;
import com.druvu.json.JsonObjectBuilder;
import com.druvu.json.JsonValue;
import com.druvu.json.Mapper;
import com.druvu.json.Yaml;
import com.druvu.json.contract.FrontDoor;

/** {@link Yaml}'s entry points, so the shared contract runs through YAML's own front door. */
final class YamlFrontDoor implements FrontDoor {

    static final FrontDoor INSTANCE = new YamlFrontDoor();

    @Override
    public JsonObjectBuilder<?> object() {
        return Yaml.object();
    }

    @Override
    public JsonArrayBuilder<?> array() {
        return Yaml.array();
    }

    @Override
    public <T> JsonArrayBuilder<?> array(Mapper<T> transform, Iterable<T> objects) {
        return Yaml.array(transform, objects);
    }

    @Override
    public JsonBuilder value(String value) {
        return Yaml.value(value);
    }

    @Override
    public JsonBuilder value(Number value) {
        return Yaml.value(value);
    }

    @Override
    public JsonBuilder value(Boolean value) {
        return Yaml.value(value);
    }

    @Override
    public JsonValue parse(String text) {
        return Yaml.parse(text);
    }
}
