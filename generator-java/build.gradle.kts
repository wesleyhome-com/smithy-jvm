plugins {
	id("buildsrc.convention.kotlin-jvm")
}

dependencies {
	implementation(project(":generator-core"))
	implementation(project(":generator-java-spi"))

	// JavaPoet for clean Java code generation
	implementation(libs.javapoet)

	// Smithy model is also needed for Java-specific shape analysis
	implementation(libs.smithy.model)

	// Unit testing for the generator
	testImplementation(platform(libs.junit.bom))
	testImplementation(libs.junit.jupiter)
	testImplementation(libs.assertk.core)
	testRuntimeOnly(libs.junit.platform.launcher)

	// Smithy testing utilities to validate models in memory
	testImplementation(libs.smithy.model)
}

java {
	sourceCompatibility = JavaVersion.VERSION_17
	targetCompatibility = JavaVersion.VERSION_17
}
