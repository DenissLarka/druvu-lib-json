package com.druvu.json.gson;

import com.druvu.json.JsonBackend;
import com.druvu.json.contract.JsonSyntaxContract;

/** Runs the JSON-text strictness contract against {@link GsonBackend}. */
public class TestGsonSyntaxContract extends JsonSyntaxContract {

    @Override
    protected JsonBackend<?> backend() {
        return new GsonBackend();
    }
}
