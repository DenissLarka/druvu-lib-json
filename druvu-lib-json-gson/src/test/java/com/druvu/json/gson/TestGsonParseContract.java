package com.druvu.json.gson;

import com.druvu.json.JsonBackend;
import com.druvu.json.contract.JsonParseContract;

/** Runs the reading contract against {@link GsonBackend}. */
public class TestGsonParseContract extends JsonParseContract {

    @Override
    protected JsonBackend<?> backend() {
        return new GsonBackend();
    }
}
