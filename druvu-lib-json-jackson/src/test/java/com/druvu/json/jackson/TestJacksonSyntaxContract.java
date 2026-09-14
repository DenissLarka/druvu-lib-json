package com.druvu.json.jackson;

import com.druvu.json.JsonBackend;
import com.druvu.json.contract.JsonSyntaxContract;

/** Runs the JSON-text strictness contract against {@link JacksonBackend}. */
public class TestJacksonSyntaxContract extends JsonSyntaxContract {

    @Override
    protected JsonBackend<?> backend() {
        return new JacksonBackend();
    }
}
