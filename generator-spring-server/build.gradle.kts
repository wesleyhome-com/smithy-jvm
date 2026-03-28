plugins {
	id("buildsrc.convention.kotlin-jvm")
	alias(libs.plugins.kotlinPluginSerialization)
}

dependencies {
	implementation(project(":generator-core"))
	implementation(project(":generator-java-spi"))
	implementation(project(":generator-java"))
	implementation(project(":generator-java-jackson"))
	implementation(project(":generator-java-validation"))
	implementation(project(":generator-java-client-http-jdk"))
	implementation(project(":generator-java-client-http-okhttp"))
	implementation(project(":generator-java-client-codec-jackson"))
	implementation(project(":generator-java-client-codec-gson"))

	// Core Smithy build and codegen abstractions
	implementation(libs.smithy.build)
	implementation(libs.smithy.codegenCore)
	implementation(libs.smithy.model)

	// JavaPoet for clean Java code generation
	implementation(libs.javapoet)

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
