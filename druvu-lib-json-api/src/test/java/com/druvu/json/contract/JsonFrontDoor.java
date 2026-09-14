package com.druvu.json.contract;

import com.druvu.json.Json;
import com.druvu.json.JsonArrayBuilder;
import com.druvu.json.JsonBuilder;
import com.druvu.json.JsonObjectBuilder;
import com.druvu.json.JsonValue;
import com.druvu.json.Mapper;

/** {@link Json}'s entry points as a {@link FrontDoor}. */
final class JsonFrontDoor implements FrontDoor {

    @Override
    public JsonObjectBuilder<?> object() {
        return Json.object();
    }

    @Override
    public JsonArrayBuilder<?> array() {
        return Json.array();
    }

    @Override
    public <T> JsonArrayBuilder<?> array(Mapper<T> transform, Iterable<T> objects) {
        return Json.array(transform, objects);
    }

    @Override
    public JsonBuilder value(String value) {
        return Json.value(value);
    }

    @Override
    public JsonBuilder value(Number value) {
        return Json.value(value);
    }

    @Override
    public JsonBuilder value(Boolean value) {
        return Json.value(value);
    }

    @Override
    public JsonValue parse(String text) {
        return Json.parse(text);
    }
}
