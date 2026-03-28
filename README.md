# Smithy-to-Spring Boot Matrix Codegen

A high-performance, multi-role Smithy Build Plugin that generates idiomatic Java (and soon Kotlin) code from Smithy
models. This generator follows a **Matrix Architecture**, allowing you to generate strictly what you need—whether it's
pure domain models, lightweight client SDKs, or full-featured Spring Boot servers.

## The Matrix Architecture

| Role       | Plugin Name          | Target Use Case                        | Primary Dependencies            |
|:-----------|:---------------------|:---------------------------------------|:--------------------------------|
| **Model**  | `java-model`         | Pure domain types (POJOs/Records)      | None (or Jackson/GSON)          |
| **Client** | `java-client`        | Lightweight, pluggable Service Clients | Pluggable (JDK, OkHttp)         |
| **Server** | `java-spring-server` | Spring Boot 3/4 Controllers & APIs     | Spring Boot, Jakarta Validation |

## Key Features

- **Modern Java 17+**: Utilizes Java Records for DTOs and Union variants, and Sealed Interfaces for polymorphic types.
- **Hybrid Granular Delegate Pattern**: The server generator enforces a strict separation between API transport (
  Controllers) and business logic (Functional Interfaces).
- **One Interface Per Operation**: Every Smithy operation generates its own functional interface. This allows for
  extreme decoupling and makes testing/implementing specific actions a breeze.
- **Safe Global Error Handling**: Generates a centralized `@ControllerAdvice` and custom exception classes with nested
  `Data` DTOs and `toDto()` mapping to prevent leaking internal stack traces.
- **Smithy 2.0 `@default` Support**: Automatically assigns default values at runtime using Spring's
  `@RequestParam(defaultValue = "...")` and Record Compact Constructors.
- **Robust Enums**: Full support for Smithy 2.0 `enum` and `intEnum` shapes, including `UNKNOWN_TO_SDK_VERSION`
  fallbacks.
- **Strict Tenet Alignment**:
    - **Service Closure**: Only generates code for shapes reachable from your target service.
    - **Renames**: Fully respects Smithy `service` renames.
    - **Protocol Agnostic**: Automatically synthesizes dedicated operation inputs/outputs via `ModelTransformer`.
- **Abstracted Client Design**: Generated clients use an **Interface + Builder** pattern with pluggable transport (
  `HttpTransport`) and serialization (`ProtocolCodec`) layers.
- **Deep Customization**: Client builders include `UnaryOperator` hooks to configure underlying library objects (e.g.,
  `HttpClient.Builder`, `ObjectMapper`).
- **Validation**: Maps Smithy constraints (`@length`, `@range`, `@pattern`) to Jakarta/Hibernate validation annotations.
- **Rich Documentation**: Extracts Smithy `@documentation` traits and applies them as Javadocs across all generated
  code.
- **Tag-Driven Domain Grouping**: Uses the Smithy `@tags` trait to map your API domains into organized Java packages and
  Spring Controllers.

## Pluggable Integrations (SPI)

The generator now uses a classpath-discovered integration SPI.

- Build third-party integrations against `generator-java-spi` only.
- Register integrations using `META-INF/services/com.wesleyhome.smithy.generator.JavaCodegenIntegration`.
- Integrations are filtered by `supports(target)` and ordered by `priority()`.
- Generator contributions are grouped by family (for example `model:structures`, `client:service`).
- Exactly one integration wins per family. If multiple integrations claim the same family at the same priority,
  generation fails fast.

Bundled integrations are split into dedicated modules:

- `generator-java-jackson`
- `generator-java-validation`

## Known Limitations / Roadmap

- **`@streaming` Trait**: Real-time streaming payloads (via `java.io.InputStream`, Reactive Streams, or Spring WebFlux)
  are not currently supported, but are actively planned for a future release.

---

## Tag-Driven Architecture

This generator uses the Smithy `@tags` trait to organize your generated codebase intelligently. By applying
`@tags(["DomainName"])` to your operations and structures, you control the resulting Java packaging and Spring
Controller layout.

### 1. Controller Grouping

Operations sharing the same primary tag are bundled into a single Spring `@RestController`.

```smithy
@tags(["Catalog"])
operation SearchCatalog { ... }

@tags(["Catalog"])
operation AddMediaItem { ... }
```

**Result:** Both API endpoints are mounted on `CatalogController.java`.

### 2. Sub-Packaging

Tags automatically drive the sub-packaging of your DTOs and API interfaces (e.g., `model.catalog`, `api.catalog`),
keeping your `import` statements clean as your service scales.

---

## Configuration (`smithy-build.json`)

Configure the plugins within your projections. All plugins require the `service` target.

### Example: Multi-Role Projection

```json
{
  "version": "2.0",
  "projections": {
    "client_sdk": {
      "plugins": {
        "java-client": {
          "service": "com.wesleyhome#MyService",
          "package": "com.wesleyhome.client",
          "serializationLibrary": "jackson",
          "httpClientLibrary": "okhttp"
        }
      }
    },
    "server_impl": {
      "plugins": {
        "java-spring-server": {
          "service": "com.wesleyhome#MyService",
          "package": "com.wesleyhome.generated"
        }
      }
    }
  }
}
```

### Configuration Options

| Option                 | Default                              | Roles  | Description                                                                              |
|:-----------------------|:-------------------------------------|:-------|:-----------------------------------------------------------------------------------------|
| `service`              | **Required**                         | All    | The Shape ID of the service to generate (e.g., `com.wesleyhome#MyService`).              |
| `package`              | `com.wesleyhome.generated`           | All    | The base Java package for generated code.                                                |
| `dtoSuffix`            | `DTO`                                | All    | Suffix added to generated structures (except Exceptions).                                |
| `serializationLibrary` | `jackson` (Server) / `none` (Client) | All    | `jackson`, `gson`, or `none`. Controls generated annotations and default adapters.       |
| `httpClientLibrary`    | `jdk`                                | Client | `jdk`, `okhttp`, or `none`. Controls which default `HttpTransport` adapter is generated. |

---

## Required Dependencies

### 1. Gradle Plugins

Your `build.gradle.kts` must include the Spring Boot and Smithy Gradle plugins:

```kotlin
plugins {
    // Spring Boot 3 or 4
    id("org.springframework.boot") version "4.0.4"
    id("io.spring.dependency-management") version "1.1.7"
    
    // Smithy Gradle Plugin
    id("software.amazon.smithy.gradle.smithy-base") version "1.4.0"
    java
}
```

### 2. Generator Dependency

You must register the generator in your `dependencies` block using the `smithyBuild` configuration:

```kotlin
dependencies {
    // Replace with the actual coordinates once published
    smithyBuild("com.wesleyhome.smithy:smithy-to-spring-boot:0.0.1")
}
```

### 3. Runtime Dependencies

**Note:** Both Spring Boot 3 and 4 require **Java 17+**.

#### For `java-spring-server`

##### Spring Boot 4

```kotlin
dependencies {
    implementation("org.springframework.boot:spring-boot-starter-webmvc") // WebMVC starter in SB4
    implementation("org.springframework.boot:spring-boot-starter-validation")
    // Jackson is required for the Server role
    implementation("com.fasterxml.jackson.core:jackson-databind")
}
```

##### Spring Boot 3

```kotlin
dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web") // Web starter in SB3
    implementation("org.springframework.boot:spring-boot-starter-validation")
    // Jackson is required for the Server role
    implementation("com.fasterxml.jackson.core:jackson-databind")
}
```

#### For `java-client` (with OkHttp & Jackson)

```kotlin
dependencies {
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.fasterxml.jackson.core:jackson-databind")
}
```

#### For `java-client` (with GSON)

```kotlin
dependencies {
    implementation("com.google.code.gson:gson:2.12.1")
}
```

---

## Project Structure (Generated Code)

The generator organizes code into a clean, role-based directory structure depending on which plugin you configure.

### Server Layout (`java-spring-server`)

```text
basePackage/
├── api/                  # Functional Interfaces for each operation (e.g., GetMediaApi.java)
│   └── stub/             # Default 501 Not Implemented stubs (loaded via ConditionalOnMissingBean)
├── controller/           # Spring RestControllers (grouped by tag, e.g., CatalogController.java)
├── config/               # Spring AutoConfiguration for Fallback Stubs
└── model/                # Pure Java DTOs, Enums, and Unions (sub-packaged by tag)
```

### Client Layout (`java-client`)

```text
basePackage/
├── client/               # Abstracted transport/codec interfaces & specific configured adapters
│   ├── HttpTransport.java
│   ├── ProtocolCodec.java
│   ├── JdkHttpTransport.java
│   ├── JacksonCodec.java
│   ├── LibraryServiceClient.java
│   └── DefaultLibraryServiceClient.java
└── model/                # Pure Java DTOs, Enums, and Unions (sub-packaged by tag)
```

## How to use the Server Generation (The Delegate Pattern)

This plugin uses a **Hybrid Granular Delegate Pattern**. It enforces a strict separation between HTTP transport logic (
Controllers) and your business logic (Adapters).

### 1. The Generated API Interface

For every operation in your Smithy model, we generate a single functional interface. Notice how it operates strictly on
business domain objects (DTOs), completely abstracted from Spring's `ResponseEntity` or HTTP headers.

```java
// Generated by smithy-to-spring-boot
package com.wesleyhome.generated.api.catalog;

public interface SearchCatalogApi {
    SearchCatalogOutputDTO searchCatalog(String query, MediaTypeDTO type, Integer page);
}
```

### 2. The Generated Controller

We also generate a Spring `@RestController` that handles all `@PathVariable`, `@RequestHeader`, and HTTP status code
mappings. It automatically injects your implementations of the API interfaces.

```java
// Generated by smithy-to-spring-boot
@RestController
public class CatalogController {
    private final SearchCatalogApi searchCatalogApi;

    public CatalogController(SearchCatalogApi searchCatalogApi) {
        this.searchCatalogApi = searchCatalogApi;
    }

    @GetMapping("/catalog")
    public ResponseEntity<SearchCatalogOutputDTO> searchCatalog(...) {
        // Delegates to your implementation, then maps to ResponseEntity
        SearchCatalogOutputDTO result = searchCatalogApi.searchCatalog(query, type, page);
        return ResponseEntity.ok().body(result);
    }
}
```

### 3. Your Implementation (The Adapter)

To implement the API, you simply create a standard Spring `@Service` or `@Component` that implements the generated
interface. You can implement one interface per class, or group multiple related interfaces into a single adapter.

```java
// Written by YOU
package com.wesleyhome.library.adapter;

import com.wesleyhome.generated.api.catalog.SearchCatalogApi;
import org.springframework.stereotype.Service;

@Service
public class CatalogApiAdapter implements SearchCatalogApi {
    
    private final MyDatabaseService dbService;
    
    public CatalogApiAdapter(MyDatabaseService dbService) {
        this.dbService = dbService;
    }

    @Override
    public SearchCatalogOutputDTO searchCatalog(String query, MediaTypeDTO type, Integer page) {
        // 1. Execute your pure business logic
        List<MediaItemDTO> items = dbService.search(query, type, page);
        
        // 2. Return the pure DTO. The controller handles the HTTP 200 OK.
        return new SearchCatalogOutputDTO(items, (long) items.size());
    }
}
```

If you add a new operation to your Smithy model but haven't implemented the interface yet, the system will automatically
fall back to a generated stub that returns an HTTP `501 Not Implemented`, ensuring your application always compiles and
runs safely.

---
