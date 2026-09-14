druvu-lib-json
==============

[![CI](https://github.com/DenissLarka/druvu-lib-json/actions/workflows/ci.yml/badge.svg?branch=main)](https://github.com/DenissLarka/druvu-lib-json/actions/workflows/ci.yml)
![Java](https://img.shields.io/badge/Java-25-blue)
[![License](https://img.shields.io/badge/license-Apache--2.0-blue)](LICENSE)

Fluent JSON building and strict parsing with pluggable backends — and the same API over YAML.

The builder DSL is forked from [Bryn Cooke's fluent-json](https://github.com/BrynCooke/fluent-json);
this library decouples it from Gson behind a small backend SPI (discovered via
[druvu-lib-loader](https://repo1.maven.org/maven2/com/druvu/druvu-lib-loader/)) and adds the
reading side: parsing into a navigable, strictly typed value tree.

| Module | Content |
|--------|---------|
| `druvu-lib-json-api` | builder + parser API, `JsonBackend` SPI — no JSON library dependency |
| `druvu-lib-json-gson` | Gson backend |
| `druvu-lib-json-jackson` | Jackson 3 backend (`tools.jackson`) |
| `druvu-lib-json-yaml` | YAML 1.2 backend (snakeyaml-engine) |

Add one JSON backend module; the api comes transitively. The backend's own library is `provided`:
**your application supplies its own Gson or Jackson version.**

```xml
<dependency>
  <groupId>com.druvu</groupId>
  <artifactId>druvu-lib-json-gson</artifactId>
  <version>1.0.0</version>
</dependency>
<dependency>
  <groupId>com.google.code.gson</groupId>
  <artifactId>gson</artifactId>
  <version>2.14.0</version>
</dependency>
```

or, in a Jackson house, `druvu-lib-json-jackson` next to `tools.jackson.core:jackson-databind`.

**Zero-config** — the backend is discovered on the class path / module path. Exactly one JSON
backend must be present: with none, the entry points fail with a clear error; with two, discovery
fails naming both (the explicit-backend entry points below work regardless). The YAML module is
not a second JSON backend — see [YAML](#yaml) — so it can sit alongside either.

Published to **GitHub Packages** — see [Installation](#installation-github-packages) for the
one-time authentication setup.

Building
--------

```java
String json = Json.object()
    .add("prop1", "1")
    .add("prop2", 2)
    .addObject("prop5")
        .add("np1", 4)
        .end()
    .addArray("prop6")
        .addObject()
            .end()
        .add("ae1")
        .end()
    .toJson();
```

```json
{
    "prop1": "1",
    "prop2": 2,
    "prop5": {
            "np1": 4
        },
    "prop6": [
            {},
            "ae1"
        ]
}
```

To map your own types while building, a `Mapper` is just a lambda:

```java
Mapper<Customer> customer = c -> Json.object().add("id", c.id()).add("name", c.name());
String json = Json.object().add("customers", customer, customers).toJson();
```

Parsing
-------

`Json.parse` returns a `JsonValue` — an object, an array, or a primitive — navigated with typed
accessors:

```java
JsonObject root = Json.parse(payload).asObject();

String id        = root.string("id");
boolean active   = root.bool("active");
BigDecimal total = root.decimal("total");
long sequence    = root.longValue("sequence");

for (JsonValue item : root.array("items")) {
    JsonObject o = item.asObject();
    // ...
}
```

The accessors are deliberately strict:

- **Nothing is coerced.** Asking a string for a number throws `JsonException`, and every error
  names its location: `Expected NUMBER but found STRING at $.items[0].name`. A missing key lists
  the keys that are present.
- **Numbers are `BigDecimal`** — exactly as they appeared on the wire, never through floating
  point. `asInt()`/`asLong()` succeed only when the value is exactly representable; there is no
  `double` accessor.
- **A `null` on the wire reads as "not given".** `get(key)` demands the key; `find(key)` returns
  `Optional.empty()` both for a missing key and for a key whose value is `null`, so optional
  fields read as `find("closedAt").map(JsonValue::asString)`. The raw facts stay reachable for
  the rare caller who needs them: `has(key)` is true for a null-valued key, and
  `get(key).isNull()` tells. The write side mirrors it — `null` is refused, never built: omit
  what you do not have.
- **Parsing is strict RFC 8259.** Malformed documents, trailing content, single quotes and
  unquoted keys are refused.

The hierarchy is sealed, so it pattern-matches exhaustively:

```java
switch (Json.parse(input)) {
    case JsonObject o -> ...
    case JsonArray a -> ...
    case JsonPrimitive p -> ...
}
```

Building and parsing meet in `JsonValue`: `build()` returns the same type `parse` produces, so a
built tree is navigable without serializing, and a parsed fragment grafts into a builder:

```java
JsonObject built = Json.object().add("a", "b").build().asObject();  // no casts anywhere

JsonValue instrument = Json.parse(fragment);
String order = Json.object().add("instrument", instrument).toJson();
```

YAML
----

`druvu-lib-json-yaml` reads and writes YAML 1.2 through the same builders, the same `JsonValue`
tree and the same strict accessors. Only the front door differs:

```java
String config = Yaml.object()
    .add("endpoint", url)
    .addArray("profiles").add("sandbox").add("prod").end()
    .toJson();

JsonObject parsed = Yaml.parse(config).asObject();
```

```yaml
endpoint: https://example.test
profiles:
- sandbox
- prod
```

**A format is not a backend.** Gson and Jackson are interchangeable ways to hold the same JSON, so
asking for two at once is an accident worth failing on. YAML is a different answer to *what should
this text be*, so it is chosen at the call site instead of being discovered — and a YAML module and
a JSON module coexist on one class path without either shadowing the other.

The schema is **core** — YAML 1.2, the version that is a strict superset of JSON. YAML 1.1
spellings are deliberately not honoured: `yes` and `on` are strings, `1:30` is a string and not
sexagesimal sixty, `1_000` is a string. Two further narrowings keep YAML inside the data model this
API exposes: **a mapping key must be a scalar**, and the non-finite floats YAML can spell (`.inf`,
`.nan`) are **refused** when read as a number rather than silently substituted.

Because the tree is the JSON data model throughout, `toJson()` stays the terminal that serializes a
value — as YAML text when the backend is YAML.

Explicit backend
----------------

Every entry point also takes an explicit `JsonBackend`, skipping discovery. The backend's native
tree is available from any value via `raw()` — the one deliberate escape hatch for
backend-specific interop:

```java
JsonElement gsonTree = (JsonElement) Json.object(new GsonBackend()).add("a", 1).build().raw();
```

Values and builders from different backends cannot be combined; mixing them raises an
`IllegalArgumentException`. For the same reason a `Mapper` is bound to whichever front door it
builds through, so write `s -> Json.value(s)` (or `Yaml.value`) rather than reaching for a shared
constant.

Adding a backend
----------------

1. Implement `JsonBackend<YourNodeType>` — node creation and mutation for building; parse and
   inspection for reading. Date/time conversion has an ISO-string default, override it if your
   format has native types. A backend for a *different format* implements `YamlBackend` instead, or
   follows its pattern: a marker interface plus a facade of its own
2. Implement `com.druvu.lib.loader.ComponentFactory` returning your backend
3. Register it in `META-INF/services/com.druvu.lib.loader.ComponentFactory`
   (and `provides ... with ...` in `module-info.java`)
4. Run the contract suite against it: depend on the api `test-jar` (test scope) and subclass
   `JsonBuildContract`, `JsonParseContract` and `JsonSyntaxContract` from
   `com.druvu.json.contract`, each returning your backend — the gson and jackson modules show
   the shape
5. Done — no changes to `druvu-lib-json-api`

Installation (GitHub Packages)
------------------------------

This library is published to **GitHub Packages**, which requires Maven authentication even for a
public package.

**1. Generate a GitHub Personal Access Token:**

Create a **classic** token with the single `read:packages` scope — nothing more is needed to
consume a public package.
[This link](https://github.com/settings/tokens/new?scopes=read:packages&description=maven-read-packages)
opens the form with the right type and scope pre-selected.

> Note: it must be a *classic* token. GitHub's token page defaults to the newer fine-grained
> tokens, which the GitHub Packages Maven registry does not accept — the symptom is an
> unexplained `401 Unauthorized` from `maven.pkg.github.com`.

**2. Add the server to `~/.m2/settings.xml`:**

```xml
<settings>
  <servers>
    <server>
      <id>github</id>
      <username>YOUR_GITHUB_USERNAME</username>
      <password>YOUR_GITHUB_TOKEN</password>
    </server>
  </servers>
</settings>
```

**3. Add the repository and the backend you want to your project `pom.xml`:**

```xml
<repositories>
    <repository>
        <id>github</id>
        <url>https://maven.pkg.github.com/DenissLarka/druvu-lib-json</url>
    </repository>
</repositories>
```

```xml
<dependency>
    <groupId>com.druvu</groupId>
    <artifactId>druvu-lib-json-gson</artifactId>
    <version>1.0.0</version>
</dependency>
<!-- or druvu-lib-json-jackson, or druvu-lib-json-yaml -->
```

The api comes transitively; bring your own Gson, Jackson or snakeyaml-engine version. Writing a
backend of your own? The contract suite ships as the api's `tests` artifact:

```xml
<dependency>
    <groupId>com.druvu</groupId>
    <artifactId>druvu-lib-json-api</artifactId>
    <version>1.0.0</version>
    <type>test-jar</type>
    <scope>test</scope>
</dependency>
```

License
-------

Original fluent-json Copyright 2013 Bryn Cooke.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

        http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

Feedback
--------

- Found a bug or missing a feature? [Open an issue](https://github.com/DenissLarka/druvu-lib-json/issues)
- More druvu libraries and tools: [druvu.com](https://druvu.com)
