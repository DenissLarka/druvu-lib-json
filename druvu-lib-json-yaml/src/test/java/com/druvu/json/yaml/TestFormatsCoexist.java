package com.druvu.json.yaml;

import static org.assertj.core.api.Assertions.assertThat;

import com.druvu.json.Json;
import com.druvu.json.JsonObject;
import com.druvu.json.Yaml;
import com.druvu.json.gson.GsonBackend;
import com.google.gson.JsonElement;
import org.snakeyaml.engine.v2.nodes.Node;
import org.testng.annotations.Test;

/**
 * The reason YAML has its own front door rather than being a third backend: both are on this test's class path at once,
 * and neither shadows the other. Two backends of the <em>same</em> format would still fail loud — that is the accident
 * worth refusing, and it is not this.
 *
 * @author Deniss Larka
 */
public class TestFormatsCoexist {

    @Test
    public void eachFrontDoorFindsItsOwnBackend() {
        assertThat(Json.object().build().raw()).isInstanceOf(JsonElement.class);
        assertThat(Yaml.object().build().raw()).isInstanceOf(Node.class);
    }

    @Test
    public void theSameDocumentReadsThroughBothAndAgrees() {
        String document = "{\"id\":\"C-1001\",\"tags\":[\"private\",\"chf\"]}";

        JsonObject asJson = Json.parse(document).asObject();
        JsonObject asYaml = Yaml.parse(document).asObject();

        assertThat(asJson.string("id")).isEqualTo(asYaml.string("id")).isEqualTo("C-1001");
        assertThat(asJson.array("tags").get(0).asString())
                .isEqualTo(asYaml.array("tags").get(0).asString())
                .isEqualTo("private");
    }

    @Test
    public void oneTreeCanBeEmittedAsEitherFormat() {
        // Same values, two serializations — the tree is the JSON data model either way.
        assertThat(Json.object().add("a", 1).toJson()).isEqualTo("{\"a\":1}");
        assertThat(Yaml.object().add("a", 1).toJson()).isEqualTo("a: 1\n");
    }

    @Test
    public void aYamlBackendIsAJsonBackendWhenPassedExplicitly() {
        // The explicit entry points take any JsonBackend, and YamlBackend is one.
        assertThat(Json.object(new SnakeYamlBackend()).add("a", 1).toJson()).isEqualTo("a: 1\n");
        assertThat(Json.object(new GsonBackend()).add("a", 1).toJson()).isEqualTo("{\"a\":1}");
    }
}
