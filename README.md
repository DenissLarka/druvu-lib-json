druvu-lib-json
==============

A fluent JSON builder with pluggable serialization backends.

Forked from [Bryn Cooke's fluent-json](https://github.com/BrynCooke/fluent-json).
The builder API is decoupled from Gson: backends implement a small SPI and are
discovered via [druvu-lib-loader](https://repo1.maven.org/maven2/com/druvu/druvu-lib-loader/).

| Module | Content |
|--------|---------|
| `fluent-json-api` | builder API + `Backend` SPI, no JSON library dependency |
| `fluent-json-gson` | Gson backend |

Usage
-----

Add the backend module; the api comes transitively:

```xml
<dependency>
  <groupId>com.druvu</groupId>
  <artifactId>fluent-json-gson</artifactId>
  <version>1.0.0-SNAPSHOT</version>
</dependency>
```

**Zero-config** — the backend is discovered on the classpath/module path.
Exactly one backend module must be present; with none (or several) the factory
methods fail with a clear error.

```java
String json = JsonBuilderFactory.buildObject()
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
    .toString();
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

**Explicit backend** — no discovery, and `getJson()` returns the backend's
native root node type:

```java
JsonBuilderFactory.Backend<JsonElement> backend = new GsonBackend();
JsonElement json = JsonBuilderFactory.buildObject(backend)
    .add("name", "Alice")
    .add("age", 30)
    .getJson();
```

Builders from different backends cannot be combined; mixing them raises an
`IllegalArgumentException`.

Adding a backend
----------------

1. Implement `JsonBuilderFactory.Backend<YourNodeType>` — date/time and character
   conversions have ISO-string defaults, override them if your format has native types
2. Implement `com.druvu.lib.loader.ComponentFactory` returning your backend
3. Register it in `META-INF/services/com.druvu.lib.loader.ComponentFactory`
   (and `provides ... with ...` in `module-info.java`)
4. Done — no changes to `fluent-json-api`

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
