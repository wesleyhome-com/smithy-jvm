plugins {
	id("buildsrc.convention.kotlin-jvm")
	`java-library`
}

dependencies {
	// Core Smithy build and codegen abstractions
	api(libs.smithy.build)
	api(libs.smithy.codegenCore)
	api(libs.smithy.model)
}

java {
	sourceCompatibility = JavaVersion.VERSION_17
	targetCompatibility = JavaVersion.VERSION_17
}
