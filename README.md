druvu-lib-json
==============

[![CI](https://github.com/DenissLarka/druvu-lib-json/actions/workflows/ci.yml/badge.svg?branch=main)](https://github.com/DenissLarka/druvu-lib-json/actions/workflows/ci.yml)
![Java](https://img.shields.io/badge/Java-25-blue)
[![License](https://img.shields.io/badge/license-Apache--2.0-blue)](LICENSE)

Fluent JSON building and strict parsing with pluggable backends.

The builder DSL is forked from [Bryn Cooke's fluent-json](https://github.com/BrynCooke/fluent-json);
this library decouples it from Gson behind a small backend SPI (discovered via
[druvu-lib-loader](https://repo1.maven.org/maven2/com/druvu/druvu-lib-loader/)) and adds the
reading side: parsing into a navigable, strictly typed value tree.

| Module | Content |
|--------|---------|
| `druvu-lib-json-api` | builder + parser API, `JsonBackend` SPI — no JSON library dependency |
| `druvu-lib-json-gson` | Gson backend |

Add the backend module; the api comes transitively. The backend's JSON library itself is
`provided`: **your application supplies its own Gson version.**

```xml
<dependency>
  <groupId>com.druvu</groupId>
  <artifactId>druvu-lib-json-gson</artifactId>
  <version>1.0.0-SNAPSHOT</version>
</dependency>
<dependency>
  <groupId>com.google.code.gson</groupId>
  <artifactId>gson</artifactId>
  <version>2.13.2</version>
</dependency>
```

**Zero-config** — the backend is discovered on the class path / module path. Exactly one backend
module must be present; with none, the entry points fail with a clear error.

> Pre-release: not yet published to a public repository.

Building
--------

```java
String json = Json.object()
    .add("prop1", "1")
    .add("prop2", 2)
    .addNull("prop3")
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
    "prop3": null,
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
- **Absence and JSON `null` stay distinguishable.** `get(key)` demands the key; `find(key)`
  returns `Optional.empty()` for a missing key and a present *null value* for an explicit
  `null` — so `find("closedAt").filter(v -> !v.isNull())` reads optional fields tolerantly
  without losing the distinction.
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

Explicit backend
----------------

Every entry point also takes an explicit `JsonBackend`, skipping discovery. The backend's native
tree is available from any value via `raw()` — the one deliberate escape hatch for
backend-specific interop:

```java
JsonElement gsonTree = (JsonElement) Json.object(new GsonBackend()).add("a", 1).build().raw();
```

Values and builders from different backends cannot be combined; mixing them raises an
`IllegalArgumentException`.

Adding a backend
----------------

1. Implement `JsonBackend<YourNodeType>` — node creation and mutation for building; parse and
   inspection for reading. Date/time conversion has an ISO-string default, override it if your
   format has native types
2. Implement `com.druvu.lib.loader.ComponentFactory` returning your backend
3. Register it in `META-INF/services/com.druvu.lib.loader.ComponentFactory`
   (and `provides ... with ...` in `module-info.java`)
4. Done — no changes to `druvu-lib-json-api`

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
