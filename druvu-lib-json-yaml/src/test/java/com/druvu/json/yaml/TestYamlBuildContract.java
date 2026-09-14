package com.druvu.json.yaml;

import com.druvu.json.JsonBackend;
import com.druvu.json.contract.FrontDoor;
import com.druvu.json.contract.JsonBuildContract;

/** Runs the building contract against {@link SnakeYamlBackend}, through YAML's own front door. */
public class TestYamlBuildContract extends JsonBuildContract {

    @Override
    protected JsonBackend<?> backend() {
        return new SnakeYamlBackend();
    }

    @Override
    protected FrontDoor frontDoor() {
        return YamlFrontDoor.INSTANCE;
    }
}
