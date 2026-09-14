package com.druvu.json.jackson;

import com.druvu.json.JsonBackend;
import com.druvu.json.contract.JsonParseContract;

/** Runs the reading contract against {@link JacksonBackend}. */
public class TestJacksonParseContract extends JsonParseContract {

    @Override
    protected JsonBackend<?> backend() {
        return new JacksonBackend();
    }
}
