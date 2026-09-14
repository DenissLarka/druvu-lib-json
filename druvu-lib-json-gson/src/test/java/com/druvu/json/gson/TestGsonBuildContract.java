package com.druvu.json.gson;

import com.druvu.json.JsonBackend;
import com.druvu.json.contract.JsonBuildContract;

/** Runs the building contract against {@link GsonBackend}. */
public class TestGsonBuildContract extends JsonBuildContract {

    @Override
    protected JsonBackend<?> backend() {
        return new GsonBackend();
    }
}
