# Contributing

This document is for contributors and maintainers of the generator itself.

## Project Layout

- `generator-core`: shared Smithy/codegen utilities.
- `generator-java/base`: common Java generator infrastructure.
- `generator-java/model`: `java-model` Smithy build plugin.
- `generator-java/client`: `java-client` Smithy build plugin.
- `generator-java/spring-server`: `java-spring-server` Smithy build plugin.
- `generator-java/spi`: integration SPI interfaces.
- `generator-java/jackson`: Jackson integration module.
- `generator-java/validation`: Jakarta validation integration module.
- `generator-java/client-http-jdk`: JDK HTTP transport integration for clients.
- `generator-java/client-http-okhttp`: OkHttp transport integration for clients.
- `generator-java/client-codec-jackson`: Jackson protocol codec integration for clients.
- `generator-java/client-codec-gson`: Gson protocol codec integration for clients.
- `examples/`: sample projects that consume the generator modules.

## Local Development Setup

The examples use project dependencies so plugin changes are immediately testable:

```kotlin
dependencies {
    smithyBuild(project(":generator-java-model"))
    smithyBuild(project(":generator-java-client"))
    smithyBuild(project(":generator-java-spring-server"))
    smithyBuild(project(":generator-java-jackson"))
    smithyBuild(project(":generator-java-validation"))
    smithyBuild(project(":generator-java-client-http-jdk"))
    smithyBuild(project(":generator-java-client-http-okhttp"))
    smithyBuild(project(":generator-java-client-codec-jackson"))
    smithyBuild(project(":generator-java-client-codec-gson"))
}
```

## Build and Test

Run from repo root:

```powershell
.\gradlew build
```

For integration tests in the sample service:

```powershell
.\gradlew :sample-library-service:integrationTest
```
