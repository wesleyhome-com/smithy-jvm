# smithy-jvm

Code generators for Smithy models targeting:

- `java-model`
- `java-client`
- `java-spring-server`

## New To Smithy?

Smithy is an interface definition language (IDL) and tooling ecosystem for modeling APIs, then generating clients,
servers, and types from that model.

Official docs:

- Smithy docs home: https://smithy.io/
- Smithy `smithy-build.json` guide: https://smithy.io/2.0/guides/smithy-build-json.html
- Smithy service shape spec (`service`, operations, resources): https://smithy.io/2.0/spec/service-types.html

In this project, your Smithy model is the contract. `smithy-build.json` tells Smithy which generator plugin to run and
what settings to pass.

## What This Project Generates

This project generates Java source from a Smithy `service` closure with a plugin-per-target model:

- `java-model` generates domain model code only.
- `java-client` generates model code plus a transport/codec-pluggable client SDK.
- `java-spring-server` generates model code plus Spring MVC API interfaces, controllers, stubs, fallback config, and
  global exception handling.

All generators are driven by:

- service-closure generation (only shapes reachable from configured `service`)
- dedicated operation input/output synthesis
- resource-first package grouping (with `@tags` fallback for unbound operations)
- integration modules discovered through Java `ServiceLoader`

## Architecture Summary

The generator runtime is split into:

- **Generator plugins** (`java-model`, `java-client`, `java-spring-server`)
- **Base generators** (structures, exceptions, enums, unions, client/server emitters)
- **Enhancement integrations** (Jackson, validation, client transport, client codec)
- **SPI** (`JavaCodegenIntegration` / `JavaPoetCodegenIntegration`) for extension and overrides

Integrations contribute generators by **family**. One winner per family is selected by priority. If multiple
integrations claim the same family at the same priority, generation fails fast.

## Plugin Settings (`smithy-build.json`)

`smithy-build.json` is the build configuration file Smithy reads when you run code generation.

Minimal schema shape used by this project:

```json
{
  "version": "2.0",
  "projections": {
    "my_projection_name": {
      "plugins": {
        "java-spring-server": {
          "service": "com.example#MyService",
          "package": "com.example.generated.server",
          "dtoSuffix": "DTO"
        }
      }
    }
  }
}
```

Important fields to understand:

- `version`: Smithy build config version.
- `projections`: named generation configurations. Each projection can run different plugins/settings.
- `plugins`: map of plugin id to plugin settings (`java-model`, `java-client`, `java-spring-server`).

Projection names (`projections.<name>`) are developer-defined labels:

- You can name them anything meaningful (`server`, `client_jackson_jdk`, `model_only`, etc.).
- The name is used in generated output paths under the Smithy output directory.
  Example: `<smithyOutput>/server/java-spring-server/` or `<smithyOutput>/client/java-client/`.
- Use multiple projections when you want multiple generated variants from the same model in one build.

All three smithy-jvm plugins support the same core settings:

| Setting     | Required | Default | Description                                                                                |
|:------------|:---------|:--------|:-------------------------------------------------------------------------------------------|
| `service`   | Yes      | none    | Smithy service shape id, for example `com.example#MyService`.                              |
| `package`   | Yes      | none    | Base package for generated code.                                                           |
| `dtoSuffix` | No       | `DTO`   | Suffix for generated structure and intEnum/enum model types (exceptions are not suffixed). |

Setting details:

- `service`: the root Smithy `service` shape to generate from. Format is `namespace#ServiceName`.
  Only shapes reachable from that service closure are generated.
- `package`: base Java package for generated output. Domain-based segments (for example `patron`) may be appended
  during generation.
- `dtoSuffix`: appended to generated structure and enum type names.
  Example: `Book` -> `BookDTO` when `dtoSuffix` is `DTO`, or `Book` -> `Book` when `dtoSuffix` is `""`.

Current code does **not** define other user-facing plugin settings.

## Plugin Catalog

### `java-model` (target: MODEL)

Plugin id in `smithy-build.json`: `java-model`

What it generates:

- Java records for Smithy structures (except `@error` structures)
- Java runtime exceptions for Smithy `@error` structures, including nested `Data` DTO and `toDto()`
- Java enums for `enum` and `intEnum`, including `UNKNOWN_TO_SDK_VERSION`
- Java sealed interfaces for unions with nested variants and `Unknown`

Model behavior highlights:

- Smithy `@default` maps to record compact constructor default assignment.
- Smithy `@documentation` maps to Javadoc.
- Smithy `@deprecated` maps to `@Deprecated`.
- Primitive/shape mapping includes `timestamp -> Instant`, `document -> String`, `bigInteger -> BigInteger`,
  `bigDecimal -> BigDecimal`.

### `java-client` (target: CLIENT)

Plugin id in `smithy-build.json`: `java-client`

What it generates:

- Everything from model generation for client target families
- Core client abstractions in `...client` package:
  - `HttpRequest`
  - `HttpResponse`
  - `HttpTransport`
  - `ProtocolCodec`
- Service client API and implementation:
  - `<Service>Client` interface
  - `Default<Service>Client` package-private implementation
  - `Builder` interface and package-private builder implementation

Client behavior highlights:

- Operation methods generated from `@http` trait.
- URI label substitution for `@httpLabel`.
- Query string construction for `@httpQuery` members.
- Header mapping for `@httpHeader` input members.
- Output header extraction for `@httpHeader` output members.
- Throws `IOException` for request/codec failures.
- Requires transport and codec implementations to be configured on builder unless generated by integration modules.

### `java-spring-server` (target: SERVER)

Plugin id in `smithy-build.json`: `java-spring-server`

What it generates:

- Everything from model generation for server target families
- API interfaces per operation: `...api/<Operation>Api`
- Default 501 stubs: `...api.stub/<Operation>ApiStub`
- Controllers grouped by resolved domain: `...<domain>.controller/*Controller`
- Global exception handler: `...controller/GlobalExceptionHandler`
- Auto-configuration fallback class: `...config/SpringDelegateFallbackConfiguration`
- Spring Boot auto-config imports entry:
  - `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`

Server behavior highlights:

- Uses `TopDownIndex.getContainedOperations(service)` so resource-bound operations are included.
- Controller methods map transport concerns (`@PathVariable`, `@RequestParam`, `@RequestHeader`, `@RequestBody`).
- API interfaces are business-level signatures returning DTOs (not `ResponseEntity`).
- Controllers adapt API return values into `ResponseEntity`.
- If multiple payload members exist for a Spring controller input, generator emits validation error
  `MultiplePayloadMembers`.

#### Implementing your own API components (iterative approach)

Generated server code is designed so you can start with a running app and replace operation logic gradually.

How bean resolution works:

- For each operation, generator creates an API interface and a stub implementation in `...api.stub`.
- Stub classes are annotated with `@ConditionalOnMissingBean(<Operation>Api.class)`.
- `SpringDelegateFallbackConfiguration` also contributes fallback beans with the same condition.
- Net result: if your app defines a bean implementing `<Operation>Api`, Spring will inject your bean and skip fallback.
  Otherwise, requests hit the generated stub and return HTTP 501.

Recommended rollout sequence:

1. Generate and boot the server with only generated code. Endpoints exist, but unimplemented operations return 501.
2. Pick one operation and create a Spring bean implementing that operation's generated `...api/<Operation>Api`.
3. Keep business dependencies behind your implementation bean (repository, downstream client, mapper, etc.).
4. Repeat operation-by-operation until all critical paths are implemented.

Example custom operation implementation:

```java
package com.example.generated.patron.api.impl;

import com.example.generated.patron.api.GetPatronApi;
import com.example.generated.patron.model.GetPatronOutputDTO;
import org.springframework.stereotype.Service;

@Service
public class GetPatronApiImpl implements GetPatronApi {
    private final PatronService patronService;

    public GetPatronApiImpl(PatronService patronService) {
        this.patronService = patronService;
    }

    @Override
    public GetPatronOutputDTO getPatron(String patronId) {
        return patronService.fetchPatron(patronId);
    }
}
```

Practical guidance:

- Treat generated `...api` interfaces as your stable application boundary.
- Keep custom code outside generated source roots so regeneration does not overwrite implementations.
- If you have multiple candidates for the same API interface, use `@Primary` or `@Qualifier` explicitly.
- Throw generated Smithy error exceptions from your implementation when appropriate; generated
  `GlobalExceptionHandler` maps them to HTTP status + DTO response body.
- Implement and test one operation at a time (slice tests with `@WebMvcTest` or integration tests via `MockMvc`).

## Integration Modules

Integrations are classpath-driven. You add/remove behavior by including/removing integration modules from `smithyBuild`.

### JavaPoet integrations

| Module                      | Applies To                         | What It Adds                                                                                                                                       |
|:----------------------------|:-----------------------------------|:---------------------------------------------------------------------------------------------------------------------------------------------------|
| `generator-java-jackson`    | model/client/server model emitters | Jackson annotations on record members and exception DTO members, enum JSON creator/value/default markers, union `@JsonTypeInfo` / `@JsonSubTypes`. |
| `generator-java-validation` | model/client/server model emitters | Jakarta/Hibernate validation annotations from Smithy constraints (`@required`, `@length`, `@range`, `@pattern`) plus nested `@Valid` propagation.  |

### Client transport/codec generator integrations

| Module                                | Applies To | Generates          |
|:--------------------------------------|:-----------|:-------------------|
| `generator-java-client-http-jdk`      | client     | `JdkHttpTransport` |
| `generator-java-client-http-okhttp`   | client     | `OkHttpTransport`  |
| `generator-java-client-codec-jackson` | client     | `JacksonCodec`     |
| `generator-java-client-codec-gson`    | client     | `GsonCodec`        |

If you do not include transport/codec integrations, client core interfaces are still generated, but you must provide
your own `HttpTransport` and `ProtocolCodec` implementation at runtime.

## Resource-First Packaging Rules

Packaging is resolved with `ResourcePackageResolver`:

1. For resource-bound operations, root resource domain key is authoritative.
2. Nested resources collapse to root resource domain package.
3. Shapes referenced by resource-bound operations are grouped into that domain.
4. For unbound operations/shapes, `@tags` first value is used as fallback domain key.
5. If no domain key resolves, generation falls back to base package (no domain segment).

Example output pattern for domain `patron`:

- `com.example.generated.patron.model`
- `com.example.generated.patron.api`
- `com.example.generated.patron.api.stub`
- `com.example.generated.patron.controller`

## Dependency Setup

Use these dependencies in the project where you run Smithy code generation.

### 1. Apply Smithy Gradle plugin

```kotlin
plugins {
    java
    id("software.amazon.smithy.gradle.smithy-base") version "1.4.0"
}
```

### 2. Add generator modules to `smithyBuild`

Set a single version and use published coordinates:

```kotlin
val smithyJvmVersion = "0.1.0-SNAPSHOT"
```

Choose modules based on target and integrations you want.

Model only:

```kotlin
dependencies {
    smithyBuild("com.wesleyhome.smithy:generator-java-model:$smithyJvmVersion")
}
```

Client, bare (no generated transport/codec implementation):

```kotlin
dependencies {
    smithyBuild("com.wesleyhome.smithy:generator-java-client:$smithyJvmVersion")
}
```

Client, JDK transport + Jackson codec:

```kotlin
dependencies {
    smithyBuild("com.wesleyhome.smithy:generator-java-client:$smithyJvmVersion")
    smithyBuild("com.wesleyhome.smithy:generator-java-client-http-jdk:$smithyJvmVersion")
    smithyBuild("com.wesleyhome.smithy:generator-java-client-codec-jackson:$smithyJvmVersion")
}
```

Client, JDK transport + Gson codec:

```kotlin
dependencies {
    smithyBuild("com.wesleyhome.smithy:generator-java-client:$smithyJvmVersion")
    smithyBuild("com.wesleyhome.smithy:generator-java-client-http-jdk:$smithyJvmVersion")
    smithyBuild("com.wesleyhome.smithy:generator-java-client-codec-gson:$smithyJvmVersion")
}
```

Client, OkHttp transport + Jackson codec:

```kotlin
dependencies {
    smithyBuild("com.wesleyhome.smithy:generator-java-client:$smithyJvmVersion")
    smithyBuild("com.wesleyhome.smithy:generator-java-client-http-okhttp:$smithyJvmVersion")
    smithyBuild("com.wesleyhome.smithy:generator-java-client-codec-jackson:$smithyJvmVersion")
}
```

Spring server with Jackson + validation:

```kotlin
dependencies {
    smithyBuild("com.wesleyhome.smithy:generator-java-spring-server:$smithyJvmVersion")
    smithyBuild("com.wesleyhome.smithy:generator-java-jackson:$smithyJvmVersion")
    smithyBuild("com.wesleyhome.smithy:generator-java-validation:$smithyJvmVersion")
}
```

### 3. Runtime dependencies (consumer app)

Add runtime libraries that correspond to what your generated code uses.

For Spring server generation:

```kotlin
dependencies {
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("com.fasterxml.jackson.core:jackson-databind")
}
```

For client with JDK transport + Jackson codec:

```kotlin
dependencies {
    implementation("com.fasterxml.jackson.core:jackson-databind")
}
```

For client with JDK transport + Gson codec:

```kotlin
dependencies {
    implementation("com.google.code.gson:gson:2.12.1")
}
```

For client with OkHttp transport + Jackson codec:

```kotlin
dependencies {
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.fasterxml.jackson.core:jackson-databind")
}
```

### 4. Add generated output to Gradle source sets

Smithy writes generated files to the configured output directory. Add those directories to your Gradle source sets so
the generated Java is compiled with your application code.

```kotlin
val generatedDirectory = layout.buildDirectory.get().dir("generated/sources")
val smithyOutput: Directory = generatedDirectory.dir("smithy")

smithy {
    outputDirectory.set(smithyOutput)
}

sourceSets {
    main {
        java {
            // projection name = "server", plugin id = "java-spring-server"
            srcDir(smithyOutput.dir("server/java-spring-server/"))
        }
        resources {
            srcDir("model")
        }
    }
}

tasks.processResources {
    dependsOn("smithyBuild")
}

tasks.compileJava {
    dependsOn("smithyBuild")
}
```

For complete wiring examples (including multiple projections), see:

- `examples/sample-library-client-bare/build.gradle.kts`
- `examples/sample-library-service/build.gradle.kts`

## `smithy-build.json` Examples

### Model projection

```json
{
  "version": "2.0",
  "projections": {
    "model_only": {
      "plugins": {
        "java-model": {
          "service": "com.example#MyService",
          "package": "com.example.generated.model",
          "dtoSuffix": "DTO"
        }
      }
    }
  }
}
```

### Client projection

```json
{
  "version": "2.0",
  "projections": {
    "client": {
      "plugins": {
        "java-client": {
          "service": "com.example#MyService",
          "package": "com.example.generated.client",
          "dtoSuffix": "DTO"
        }
      }
    }
  }
}
```

### Spring server projection

```json
{
  "version": "2.0",
  "projections": {
    "server": {
      "plugins": {
        "java-spring-server": {
          "service": "com.example#MyService",
          "package": "com.example.generated.server"
        }
      }
    }
  }
}
```

## Generated Output Layout

### `java-model`

```text
<basePackage>/
`-- [<domain>/]model/
    |-- StructureDTO.java
    |-- ErrorName.java
    |-- EnumTypeDTO.java
    `-- UnionTypeDTO.java
```

### `java-client`

```text
<basePackage>/
|-- [<domain>/]model/...
`-- client/
    |-- HttpRequest.java
    |-- HttpResponse.java
    |-- HttpTransport.java
    |-- ProtocolCodec.java
    |-- <Service>Client.java
    |-- Default<Service>Client.java
    |-- Default<Service>ClientBuilder.java
    |-- JdkHttpTransport.java         (if module included)
    |-- OkHttpTransport.java          (if module included)
    |-- JacksonCodec.java             (if module included)
    `-- GsonCodec.java                (if module included)
```

### `java-spring-server`

```text
<basePackage>/
|-- [<domain>/]model/...
|-- [<domain>/]api/
|   |-- <Operation>Api.java
|   `-- stub/<Operation>ApiStub.java
|-- [<domain>/]controller/
|   `-- <Domain>Controller.java
|-- controller/
|   `-- GlobalExceptionHandler.java
|-- config/
|   `-- SpringDelegateFallbackConfiguration.java
`-- META-INF/spring/
    `-- org.springframework.boot.autoconfigure.AutoConfiguration.imports
```

## Extension SPI (Advanced Users)

If you are building custom integrations:

- Implement `JavaCodegenIntegration` for lifecycle hooks and generator-family contributions.
- Implement `JavaPoetCodegenIntegration` for JavaPoet-level declaration mutation hooks.
- Register via `META-INF/services/com.wesleyhome.smithy.generator.JavaCodegenIntegration`.
- Scope integrations by `supports(target)`.
- Use `priority()` to override a family winner.

Family constants live in `GeneratorFamilies` and define the override boundary.

## Examples In This Repository

See runnable examples:

- `examples/sample-library-service`
- `examples/sample-library-client-bare`
- `examples/sample-library-client-jackson-jdk`
- `examples/sample-library-client-gson-jdk`
- `examples/sample-library-client-okhttp-jackson`

These examples are the reference for current, supported composition patterns.
