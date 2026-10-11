package com.druvu.json.yaml;

import static org.assertj.core.api.Assertions.assertThat;

import com.druvu.json.JsonObject;
import com.druvu.json.Yaml;
import org.testng.annotations.Test;

/** The root-level body entry points through the YAML front door: a document written as bodies is the same tree. */
public class TestYamlBodies {

    @Test
    public void aMappingIsWrittenAsBodies() {
        JsonObject built = Yaml.object(root -> {
                    root.add("a", 1);
                    root.addObject("o", o -> o.add("x", true));
                    root.addArray("list", l -> l.add("one").addObject(e -> e.add("k", "v")));
                })
                .build()
                .asObject();
        assertThat(built.intValue("a")).isEqualTo(1);
        assertThat(built.object("o").bool("x")).isTrue();
        assertThat(built.array("list").get(1).asObject().string("k")).isEqualTo("v");
        assertThat(Yaml.array(l -> l.add(1).add(2)).build().asArray().size()).isEqualTo(2);
    }
}
