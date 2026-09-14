package com.druvu.json.jackson;

import com.druvu.json.JsonBackend;
import com.druvu.json.contract.JsonBuildContract;

/** Runs the building contract against {@link JacksonBackend}. */
public class TestJacksonBuildContract extends JsonBuildContract {

    @Override
    protected JsonBackend<?> backend() {
        return new JacksonBackend();
    }
}
