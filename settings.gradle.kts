// The settings file is the entry point of every Gradle build.
// Its primary purpose is to define the subprojects.
// It is also used for some aspects of project-wide configuration, like managing plugins, dependencies, etc.
// https://docs.gradle.org/current/userguide/settings_file_basics.html

dependencyResolutionManagement {
	// Use Maven Central as the default repository (where Gradle will download dependencies) in all subprojects.
	@Suppress("UnstableApiUsage")
	repositories {
		mavenCentral()
	}
}

plugins {
	// Use the Foojay Toolchains plugin to automatically download JDKs required by subprojects.
	id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

// Include the `app` and `utils` subprojects in the build.
// If there are changes in only one of the projects, Gradle will rebuild only the one that has changed.
// Learn more about structuring projects with Gradle - https://docs.gradle.org/8.7/userguide/multi_project_builds.html
include(
	":generator-java-spring-server",
	":generator-core",
	":generator-java-spi",
	":generator-java-jackson",
	":generator-java-validation",
	":generator-java-client-http-jdk",
	":generator-java-client-http-okhttp",
	":generator-java-client-codec-jackson",
	":generator-java-client-codec-gson",
	":generator-java-base",
	":generator-java-model",
	":generator-java-client",
	":gradle-plugin",
	":maven-plugin",
	":test-projects:gradle-project"
)

project(":generator-java-spring-server").projectDir = file("generator-java/spring-server")
project(":generator-java-base").projectDir = file("generator-java/base")
project(":generator-java-spi").projectDir = file("generator-java/spi")
project(":generator-java-jackson").projectDir = file("generator-java/jackson")
project(":generator-java-validation").projectDir = file("generator-java/validation")
project(":generator-java-client-http-jdk").projectDir = file("generator-java/client-http-jdk")
project(":generator-java-client-http-okhttp").projectDir = file("generator-java/client-http-okhttp")
project(":generator-java-client-codec-jackson").projectDir = file("generator-java/client-codec-jackson")
project(":generator-java-client-codec-gson").projectDir = file("generator-java/client-codec-gson")
project(":generator-java-model").projectDir = file("generator-java/model")
project(":generator-java-client").projectDir = file("generator-java/client")

rootProject.name = "smithy-to-spring-boot"
