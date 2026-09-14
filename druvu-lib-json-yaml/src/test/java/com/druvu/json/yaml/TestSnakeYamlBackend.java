package com.druvu.json.yaml;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.druvu.json.JsonException;
import com.druvu.json.JsonObject;
import com.druvu.json.JsonValue;
import com.druvu.json.Yaml;
import java.io.IOException;
import java.io.StringWriter;
import java.math.BigDecimal;
import org.snakeyaml.engine.v2.nodes.Node;
import org.testng.annotations.Test;

/**
 * What is YAML's alone: the text is YAML, the schema is 1.2 core, and the places where YAML can say more than JSON are
 * narrowed deliberately. Everything format-neutral lives in the contract suite.
 *
 * @author Deniss Larka
 */
public class TestSnakeYamlBackend {

    @Test
    public void discoveredBackendIsSnakeYaml() {
        assertThat(Yaml.object().build().raw()).isInstanceOf(Node.class);
        assertThat(Yaml.parse("[]").raw()).isInstanceOf(Node.class);
    }

    @Test
    public void emitsBlockStyleYaml() {
        String yaml = Yaml.object()
                .add("name", "Joe")
                .addArray("tastes")
                .add("chicken")
                .add("pasta")
                .end()
                .toJson();

        assertThat(yaml).isEqualTo("""
                name: Joe
                tastes:
                - chicken
                - pasta
                """);
    }

    @Test
    public void readsJsonText() {
        // YAML 1.2 is a superset of JSON — the same document, the same tree.
        JsonObject root = Yaml.parse("{\"a\":[1,2],\"b\":null}").asObject();

        assertThat(root.keys()).containsExactly("a", "b");
        assertThat(root.array("a").get(1).asInt()).isEqualTo(2);
        assertThat(root.find("b")).isEmpty();
    }

    /**
     * YAML 1.1 read {@code yes} as true and {@code 1:30} as ninety. The core schema does neither, and neither do we.
     */
    @Test
    public void yamlOneOneSpellingsAreStrings() {
        assertThat(Yaml.parse("yes").asString()).isEqualTo("yes");
        assertThat(Yaml.parse("on").asString()).isEqualTo("on");
        assertThat(Yaml.parse("off").asString()).isEqualTo("off");
        assertThat(Yaml.parse("1:30").asString()).isEqualTo("1:30");
    }

    @Test
    public void coreSchemaBooleansAndNullsRead() {
        assertThat(Yaml.parse("true").asBoolean()).isTrue();
        assertThat(Yaml.parse("FALSE").asBoolean()).isFalse();
        assertThat(Yaml.parse("null").isNull()).isTrue();
        assertThat(Yaml.parse("~").isNull()).isTrue();
    }

    /** The core schema spells integers in three bases; the API only ever answers in decimal. */
    @Test
    public void hexAndOctalIntegersConvert() {
        assertThat(Yaml.parse("0x1F").asDecimal()).isEqualByComparingTo(new BigDecimal("31"));
        assertThat(Yaml.parse("0o17").asDecimal()).isEqualByComparingTo(new BigDecimal("15"));
    }

    /** Only the decimal base takes a sign, and YAML 1.1's digit separators are gone — both read as strings. */
    @Test
    public void signedHexAndUnderscoredDigitsAreStrings() {
        assertThat(Yaml.parse("-0x10").asString()).isEqualTo("-0x10");
        assertThat(Yaml.parse("0X1F").asString()).isEqualTo("0X1F");
        assertThat(Yaml.parse("1_000").asString()).isEqualTo("1_000");
    }

    @Test
    public void signsAndBareDotsRead() {
        assertThat(Yaml.parse("+5").asInt()).isEqualTo(5);
        assertThat(Yaml.parse("-5").asInt()).isEqualTo(-5);
        assertThat(Yaml.parse(".5").asDecimal()).isEqualByComparingTo(new BigDecimal("0.5"));
        assertThat(Yaml.parse("5.").asDecimal()).isEqualByComparingTo(new BigDecimal("5"));
    }

    @Test
    public void nonFiniteFloatsAreRefusedAsNumbers() {
        assertThatThrownBy(() -> Yaml.parse(".inf").asDecimal())
                .isInstanceOf(JsonException.class)
                .hasMessageContaining("Not a finite number");
        assertThatThrownBy(() -> Yaml.parse("-.inf").asDecimal()).isInstanceOf(JsonException.class);
        assertThatThrownBy(() -> Yaml.parse(".nan").asDecimal()).isInstanceOf(JsonException.class);
    }

    /** A string that would read back as a number, a boolean or nothing at all has to be quoted to stay a string. */
    @Test
    public void ambiguousStringsAreQuotedAndRoundTrip() {
        String yaml = Yaml.object()
                .add("bool", "true")
                .add("number", "42")
                .add("empty", "")
                .add("word", "yes")
                .toJson();

        assertThat(yaml).isEqualTo("""
                bool: "true"
                number: "42"
                empty: ""
                word: yes
                """);

        JsonObject reparsed = Yaml.parse(yaml).asObject();
        assertThat(reparsed.string("bool")).isEqualTo("true");
        assertThat(reparsed.string("number")).isEqualTo("42");
        assertThat(reparsed.string("empty")).isEmpty();
        assertThat(reparsed.string("word")).isEqualTo("yes");
    }

    @Test
    public void aSecondDocumentIsRefused() {
        assertThatThrownBy(() -> Yaml.parse("a: 1\n---\nb: 2"))
                .isInstanceOf(JsonException.class)
                .hasMessageContaining("single document");
    }

    @Test
    public void nonScalarKeysAreRefused() {
        assertThatThrownBy(() -> Yaml.parse("? [a, b]\n: value")).isInstanceOf(JsonException.class);
    }

    @Test
    public void commentsAreIgnored() {
        JsonObject root =
                Yaml.parse("# a leading comment\na: 1 # and a trailing one\n").asObject();
        assertThat(root.intValue("a")).isEqualTo(1);
    }

    @Test
    public void aliasesResolveToTheAnchoredValue() {
        JsonObject root = Yaml.parse("base: &b {x: 1}\ncopy: *b").asObject();

        assertThat(root.object("base").intValue("x")).isEqualTo(1);
        assertThat(root.object("copy").intValue("x")).isEqualTo(1);
    }

    @Test
    public void writeLeavesTheCallersWriterOpen() throws IOException {
        ClosableProbe out = new ClosableProbe();
        Yaml.object().add("a", 1).write(out);

        assertThat(out.toString()).isEqualTo("a: 1\n");
        assertThat(out.closed).isFalse();
    }

    @Test
    public void scalarDocumentsEmitAsPlainYaml() {
        assertThat(Yaml.value(42).toJson()).isEqualTo("42\n");
        assertThat(Yaml.value("x").toJson()).isEqualTo("x\n");
        assertThat(Yaml.value(false).toJson()).isEqualTo("false\n");
    }

    /** Engine nodes carry no structural equality, so a YAML-backed value is only ever equal to itself. */
    @Test
    public void valuesDoNotCompareStructurally() {
        JsonValue one = Yaml.parse("a: 1");
        JsonValue other = Yaml.parse("a: 1");

        assertThat(one).isNotEqualTo(other).isEqualTo(one);
    }

    private static final class ClosableProbe extends StringWriter {
        private boolean closed;

        @Override
        public void close() {
            closed = true;
        }
    }
}
