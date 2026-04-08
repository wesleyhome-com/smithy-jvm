plugins {
	// The Kotlin DSL plugin provides a convenient way to develop convention plugins.
	// Convention plugins are located in `src/main/kotlin`, with the file extension `.gradle.kts`,
	// and are applied in the project's `build.gradle.kts` files as required.
	`kotlin-dsl`
}

repositories {
	// Use the plugin portal to apply community plugins in convention plugins.
	mavenCentral()
	gradlePluginPortal()
}

dependencies {
	implementation(libs.kotlinGradlePlugin)
	implementation(libs.dokka.gradle.plugin)
}
