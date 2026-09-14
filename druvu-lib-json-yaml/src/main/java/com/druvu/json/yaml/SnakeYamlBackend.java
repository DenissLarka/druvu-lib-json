package com.druvu.json.yaml;

import com.druvu.json.JsonException;
import com.druvu.json.JsonValue;
import com.druvu.json.YamlBackend;
import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import org.snakeyaml.engine.v2.api.DumpSettings;
import org.snakeyaml.engine.v2.api.LoadSettings;
import org.snakeyaml.engine.v2.api.StreamDataWriter;
import org.snakeyaml.engine.v2.common.FlowStyle;
import org.snakeyaml.engine.v2.common.ScalarStyle;
import org.snakeyaml.engine.v2.composer.Composer;
import org.snakeyaml.engine.v2.nodes.MappingNode;
import org.snakeyaml.engine.v2.nodes.Node;
import org.snakeyaml.engine.v2.nodes.NodeTuple;
import org.snakeyaml.engine.v2.nodes.ScalarNode;
import org.snakeyaml.engine.v2.nodes.SequenceNode;
import org.snakeyaml.engine.v2.nodes.Tag;
import org.snakeyaml.engine.v2.parser.ParserImpl;
import org.snakeyaml.engine.v2.resolver.ScalarResolver;
import org.snakeyaml.engine.v2.scanner.StreamReader;
import org.snakeyaml.engine.v2.schema.CoreSchema;

/**
 * YAML 1.2 backend on snakeyaml-engine: nodes are the engine's {@link Node}s, so {@code raw()} returns its native tree.
 *
 * <p>The schema is <b>core</b> — YAML 1.2, the one that is a strict superset of JSON. YAML 1.1 spellings are
 * deliberately not honoured: {@code yes} and {@code on} are strings here, not booleans, and {@code 1:30} is a string,
 * not sexagesimal sixty. A reader that guessed differently would silently change a value's type, which is precisely
 * what this library refuses to do.
 *
 * <p>Two further narrowings keep YAML inside the JSON data model this API exposes: a mapping key must be a scalar, and
 * the non-finite floats YAML can spell ({@code .inf}, {@code .nan}) are refused when read as numbers.
 *
 * @author Deniss Larka
 */
public final class SnakeYamlBackend implements YamlBackend<Node> {

    private static final CoreSchema SCHEMA = new CoreSchema();
    private static final ScalarResolver RESOLVER = SCHEMA.getScalarResolver();

    private static final LoadSettings LOAD = LoadSettings.builder()
            .setSchema(SCHEMA)
            .setAllowRecursiveKeys(false)
            .setParseComments(false)
            .build();

    private static final DumpSettings DUMP = DumpSettings.builder()
            .setSchema(SCHEMA)
            .setDefaultFlowStyle(FlowStyle.BLOCK)
            .build();

    // -- building ----------------------------------------------------------

    @Override
    public Node newObject() {
        return new MappingNode(Tag.MAP, new ArrayList<>(), FlowStyle.BLOCK);
    }

    @Override
    public Node newArray() {
        return new SequenceNode(Tag.SEQ, new ArrayList<>(), FlowStyle.BLOCK);
    }

    @Override
    public Node of(String value) {
        // A string that would read back as something else — "true", "42", "" — has to be quoted to stay a string.
        ScalarStyle style = RESOLVER.resolve(value, true) == Tag.STR ? ScalarStyle.PLAIN : ScalarStyle.DOUBLE_QUOTED;
        return new ScalarNode(Tag.STR, value, style);
    }

    @Override
    public Node of(Number value) {
        return switch (value) {
            case Integer _, Long _, Short _, Byte _, BigInteger _ ->
                new ScalarNode(Tag.INT, value.toString(), ScalarStyle.PLAIN);
            default -> new ScalarNode(Tag.FLOAT, decimalOf(value).toString(), ScalarStyle.PLAIN);
        };
    }

    @Override
    public Node of(Boolean value) {
        return new ScalarNode(Tag.BOOL, value.toString(), ScalarStyle.PLAIN);
    }

    @Override
    public void setProperty(Node objectNode, String key, Node value) {
        List<NodeTuple> tuples = mapping(objectNode).getValue();
        NodeTuple tuple = new NodeTuple(of(key), value);
        for (int i = 0; i < tuples.size(); i++) {
            if (keyOf(tuples.get(i)).equals(key)) {
                tuples.set(i, tuple);
                return;
            }
        }
        tuples.add(tuple);
    }

    @Override
    public void addElement(Node arrayNode, Node element) {
        sequence(arrayNode).getValue().add(element);
    }

    // -- reading -----------------------------------------------------------

    @Override
    public Node parse(Reader in) throws IOException {
        // getSingleNode is the strict door: it refuses a stream that holds more than one document.
        Composer composer = new Composer(LOAD, new ParserImpl(LOAD, new StreamReader(LOAD, in)));
        Optional<Node> document = composer.getSingleNode();
        return document.orElseThrow(() -> new IllegalArgumentException("Empty document: expected a YAML value"));
    }

    @Override
    public JsonValue.Kind kindOf(Node node) {
        return switch (node.getNodeType()) {
            case MAPPING -> JsonValue.Kind.OBJECT;
            case SEQUENCE -> JsonValue.Kind.ARRAY;
            case SCALAR -> kindOfTag(node.getTag());
            case ANCHOR -> throw new IllegalArgumentException("Unresolved anchor node");
        };
    }

    private static JsonValue.Kind kindOfTag(Tag tag) {
        if (Tag.STR.equals(tag)) {
            return JsonValue.Kind.STRING;
        }
        if (Tag.INT.equals(tag) || Tag.FLOAT.equals(tag)) {
            return JsonValue.Kind.NUMBER;
        }
        if (Tag.BOOL.equals(tag)) {
            return JsonValue.Kind.BOOLEAN;
        }
        if (Tag.NULL.equals(tag)) {
            return JsonValue.Kind.NULL;
        }
        throw new JsonException("Tag " + tag + " is outside the YAML core schema");
    }

    @Override
    public String asString(Node node) {
        return scalar(node).getValue();
    }

    @Override
    public BigDecimal asDecimal(Node node) {
        ScalarNode value = scalar(node);
        return Tag.INT.equals(value.getTag()) ? integer(value.getValue()) : floating(value.getValue());
    }

    @Override
    public boolean asBoolean(Node node) {
        // The core schema admits exactly true/True/TRUE and false/False/FALSE.
        return Boolean.parseBoolean(scalar(node).getValue());
    }

    @Override
    public Node member(Node objectNode, String key) {
        Node found = null;
        for (NodeTuple tuple : mapping(objectNode).getValue()) {
            if (keyOf(tuple).equals(key)) {
                // A repeated key is not an error on read; the last one is what the document ends up saying.
                found = tuple.getValueNode();
            }
        }
        return found;
    }

    @Override
    public Set<String> keys(Node objectNode) {
        Set<String> keys = new LinkedHashSet<>();
        for (NodeTuple tuple : mapping(objectNode).getValue()) {
            keys.add(keyOf(tuple));
        }
        return keys;
    }

    @Override
    public int size(Node arrayNode) {
        return sequence(arrayNode).getValue().size();
    }

    @Override
    public Node element(Node arrayNode, int index) {
        return sequence(arrayNode).getValue().get(index);
    }

    // -- output ------------------------------------------------------------

    @Override
    public String serialize(Node node) {
        StringDataWriter out = new StringDataWriter();
        new org.snakeyaml.engine.v2.api.Dump(DUMP).dumpNode(node, out);
        return out.toString();
    }

    @Override
    public void write(Node node, Writer out) throws IOException {
        try {
            new org.snakeyaml.engine.v2.api.Dump(DUMP).dumpNode(node, new WriterDataWriter(out));
        } catch (UncheckedIOException e) {
            throw e.getCause();
        }
    }

    // -- helpers -----------------------------------------------------------

    private static BigDecimal decimalOf(Number value) {
        BigDecimal decimal = value instanceof BigDecimal d
                ? d
                : new BigDecimal(value.toString().trim());
        return decimal;
    }

    private static BigDecimal integer(String text) {
        // The core schema spells integers in three bases, and only the decimal one carries a sign; the API only ever
        // answers in decimal. BigDecimal reads a signed decimal literal as it stands.
        if (text.startsWith("0x")) {
            return new BigDecimal(new BigInteger(text.substring(2), 16));
        }
        if (text.startsWith("0o")) {
            return new BigDecimal(new BigInteger(text.substring(2), 8));
        }
        return new BigDecimal(text);
    }

    private static BigDecimal floating(String text) {
        String normalized = text.toLowerCase(Locale.ROOT);
        if (normalized.endsWith(".inf") || normalized.endsWith(".nan")) {
            // YAML can spell infinity and not-a-number; a BigDecimal cannot, and guessing a substitute would be a lie.
            throw new JsonException("Not a finite number: " + text);
        }
        return new BigDecimal(text);
    }

    private static String keyOf(NodeTuple tuple) {
        Node key = tuple.getKeyNode();
        if (key instanceof ScalarNode scalar) {
            return scalar.getValue();
        }
        throw new JsonException("Mapping key is not a scalar: " + key.getNodeType());
    }

    private static MappingNode mapping(Node node) {
        if (node instanceof MappingNode m) {
            return m;
        }
        throw new IllegalArgumentException("Not a mapping node: " + node.getNodeType());
    }

    private static SequenceNode sequence(Node node) {
        if (node instanceof SequenceNode s) {
            return s;
        }
        throw new IllegalArgumentException("Not a sequence node: " + node.getNodeType());
    }

    private static ScalarNode scalar(Node node) {
        if (node instanceof ScalarNode s) {
            return s;
        }
        throw new IllegalArgumentException("Not a scalar node: " + node.getNodeType());
    }

    /** Collects the emitter's output into a string. */
    private static final class StringDataWriter implements StreamDataWriter {
        private final StringBuilder text = new StringBuilder();

        @Override
        public void write(String str) {
            text.append(str);
        }

        @Override
        public void write(String str, int off, int len) {
            text.append(str, off, off + len);
        }

        @Override
        public String toString() {
            return text.toString();
        }
    }

    /** Feeds the emitter's output to the caller's writer, which the caller still owns and closes. */
    private record WriterDataWriter(Writer out) implements StreamDataWriter {

        @Override
        public void write(String str) {
            try {
                out.write(str);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        @Override
        public void write(String str, int off, int len) {
            try {
                out.write(str, off, len);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        @Override
        public void flush() {
            try {
                out.flush();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }
}
