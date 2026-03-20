# Smithy-to-Spring Delegate Codegen

A custom Smithy Build Plugin that generates idiomatic Spring Boot 3/4 Java code (Java 17+) using a **Hybrid Granular Delegate Pattern**. This generator ensures a strict separation between the API transport layer (Controllers) and your business logic (Functional Interfaces), organized dynamically by Smithy tags.

## Key Features

- **Java 17 Records**: Generates modern, immutable DTOs and Union variants using Java records.
- **Smithy 2.0 @default Support**: Automatically assigns default values at runtime using **Record Compact Constructors** and Spring's `@RequestParam(defaultValue = "...")`.
- **One Interface Per Operation**: Every Smithy operation generates its own functional interface (e.g., `GetMediaApi`). This allows for extreme decoupling and makes testing/implementing specific actions a breeze.
- **Automated JavaDocs**: Extracts `@documentation` traits from your Smithy model and applies them as rich JavaDocs to interfaces, methods, parameters, and DTOs.
- **Tag-Driven Domain Grouping**: 
    - **Controllers**: Grouped by the first tag found on operations. Operations tagged with `"Catalog"` are bundled into a `CatalogController`.
    - **Packaging**: Tags also drive sub-packaging for both `model` and `api` layers (e.g., `com.example.generated.model.catalog`).
- **Spring Boot 3/4 Native**: Full support for `@RestController`, `@PathVariable`, `@RequestParam`, `@RequestHeader`, and `@RequestBody`.
- **Jakarta & Hibernate Validation**: Maps Smithy constraints (`@length`, `@range`, `@required`, `@pattern`) to Jakarta annotations, including support for Hibernate's `@Range` and recursive `@Valid` support for nested records.
- **Polymorphic Unions**: Maps Smithy `union` types to Java `sealed interface` hierarchies with Jackson `defaultImpl` support for safe API evolution.
- **Robust Enums**: Full support for Smithy 2.0 `enum` shapes and Smithy 1.0 `@enum` traits, including `UNKNOWN_TO_SDK_VERSION` fallbacks.
- **Safe Global Error Handling**: Generates a centralized `@ControllerAdvice` and custom exception classes with nested `Data` DTOs and `toDto()` mapping to prevent leaking internal stack traces.
- **Atomic Generation**: Uses a functional pipeline to ensure the code generator only writes files if the entire Smithy model is valid, reporting all issues via native Smithy `ValidationEvents`.

## Project Structure

- `generator`: The core Kotlin logic implementing the `SmithyBuildPlugin`.
- `smithy-traits`: Custom Smithy traits (e.g., `@springDelegate`).
- `test-projects/gradle-project`: A comprehensive sample project featuring a Library Service divided into Catalog, Circulation, and Reservation domains.

## Directory Layout (Generated)

The generator organizes code into a clean three-tier structure:
```text
basePackage/
├── controller/           # Spring RestControllers (grouped by tag)
├── api/                  # Functional Interfaces (sub-packaged by tag)
│   └── catalog/
│       └── GetMediaApi.java
└── model/                # DTOs, Enums, and Unions (sub-packaged by tag)
    └── catalog/
        └── MediaItem.java
```

## Getting Started

### 1. Configure Dependencies
Your project needs Spring Web, Jakarta Validation, and Jackson annotations.

### 2. Plugin Configuration (`smithy-build.json`)
```json
{
  "version": "2.0",
  "plugins": {
    "spring-delegate-generator": {
      "package": "com.example.library.generated",
      "useResponseEntity": false
    }
  }
}
```

### 3. Smithy Modeling Best Practice
To satisfy Spring's requirement for a single `@RequestBody`, this generator enforces a **Strict Payload Rule**. If an operation has multiple members not bound to metadata (Path/Query/Header), they must be grouped into a single structure:

```smithy
// GOOD
operation CheckOutItem {
    input: CheckOutInput
}
structure CheckOutInput {
    @httpPayload
    request: CheckOutRequest
}
structure CheckOutRequest {
    @required patronId: String,
    @required itemId: String
}
```

## Example Usage

### Implementation
Simply implement the generated functional interfaces in your Spring `@Service` classes. You can implement one per class or bundle multiple related operations:

```java
@Service
public class CatalogService implements GetMediaApi, SearchCatalogApi {
    @Override
    public GetMediaOutput getMedia(String id) {
        // Your business logic here
        return new GetMediaOutput(...);
    }
}
```

## Development

Run generator tests:
```bash
./gradlew :generator:test
```

Build the sample project:
```bash
./gradlew :test-projects:gradle-project:build
```
