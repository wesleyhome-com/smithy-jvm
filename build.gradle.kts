import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask

plugins {
	id("net.researchgate.release") version "3.1.0" apply false
	id("org.jetbrains.dokka")
	id("com.github.ben-manes.versions") version "0.53.0"
}

val isReleaseInvocation = gradle.startParameter.taskNames
	.any { it.substringAfterLast(":").contains("release", ignoreCase = true) }

if (isReleaseInvocation) {
	apply(plugin = "net.researchgate.release")
}

subprojects {
	pluginManager.withPlugin("java") {
		extensions.configure(JavaPluginExtension::class.java) {
			when {
				path.startsWith(":generator-") -> {
					toolchain.languageVersion.set(JavaLanguageVersion.of(17))
					sourceCompatibility = JavaVersion.VERSION_17
					targetCompatibility = JavaVersion.VERSION_17
				}

				path.startsWith(":sample-library-") -> {
					toolchain.languageVersion.set(JavaLanguageVersion.of(25))
					sourceCompatibility = JavaVersion.VERSION_25
					targetCompatibility = JavaVersion.VERSION_25
				}
			}
		}
	}
}

pluginManager.withPlugin("net.researchgate.release") {
	extensions.configure<net.researchgate.release.ReleaseExtension>("release") {
		tagTemplate = "$name-$version"
	}
}

dependencies {
	dokka(project(":generator-core"))
	dokka(project(":generator-java-base"))
	dokka(project(":generator-java-client"))
	dokka(project(":generator-java-client-codec-gson"))
	dokka(project(":generator-java-client-codec-jackson"))
	dokka(project(":generator-java-client-http-jdk"))
	dokka(project(":generator-java-jackson"))
	dokka(project(":generator-java-model"))
	dokka(project(":generator-java-spi"))
	dokka(project(":generator-java-spring-server"))
	dokka(project(":generator-java-validation"))
}

dokka {
	dokkaPublications {
		html {
			outputDirectory = projectDir.resolve("docs")
			includes.from("README.md")
		}
	}
}

repositories { mavenCentral() }

fun isNonStable(version: String): Boolean {
	val stableKeyword = listOf("RELEASE", "FINAL", "GA").any { version.uppercase().contains(it) }
	val regex = "^[0-9,.v-]+(-r)?$".toRegex()
	val isStable = stableKeyword || regex.matches(version)
	return isStable.not()
}

tasks.withType<DependencyUpdatesTask>().configureEach {
	rejectVersionIf {
		isNonStable(candidate.version)
	}
}

tasks.register("publishGeneratorsToMavenCentral") {
	group = "publishing"
	description = "Publishes only generator modules to Maven Central."
	dependsOn(
		subprojects
			.filter { it.path.startsWith(":generator-") }
			.map { "${it.path}:publishAndReleaseToMavenCentral" }
	)
}
