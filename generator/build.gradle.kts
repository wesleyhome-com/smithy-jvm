plugins {
    // Apply the shared build logic from a convention plugin.
    // The shared code is located in `buildSrc/src/main/kotlin/kotlin-jvm.gradle.kts`.
    id("buildsrc.convention.kotlin-jvm")
    // Apply Kotlin Serialization plugin from `gradle/libs.versions.toml`.
    alias(libs.plugins.kotlinPluginSerialization)
}

dependencies {
    // Core Smithy build and codegen abstractions
    implementation(libs.smithy.build)
    implementation(libs.smithy.codegenCore)

    // Support for HTTP traits (needed to read @http, @httpPayload, etc.)
    implementation(libs.smithy.model)

    // JavaPoet for clean Java code generation
    implementation(libs.javapoet)

    // Reference to our custom traits module
    implementation(project(":smithy-traits"))

    // Unit testing for the generator
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.assertk.core)
    testRuntimeOnly(libs.junit.platform.launcher)

    // Smithy testing utilities to validate models in memory
    testImplementation(libs.smithy.model)
}

tasks.test {
    useJUnitPlatform()
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}
