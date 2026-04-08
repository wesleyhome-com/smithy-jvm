// The code in this file is a convention plugin - a Gradle mechanism for sharing reusable build logic.
// `buildSrc` is a Gradle-recognized directory and every plugin there will be easily available in the rest of the build.
package buildsrc.convention

import org.gradle.api.tasks.testing.logging.TestLogEvent

plugins {
	// Apply the Kotlin JVM plugin to add support for Kotlin in JVM projects.
	kotlin("jvm")
	`java-library`
	`maven-publish`
	signing
	id("org.jetbrains.dokka-javadoc")
}

val versionString = providers.gradleProperty("version").get()
version = versionString

repositories {
	// Use Maven Central for resolving dependencies.
	mavenLocal()
	mavenCentral()
}
val dokkaJavadocJar: Jar by tasks.register<Jar>("javadocJar") {
	dependsOn(tasks.dokkaGenerateModuleJavadoc)
	from(tasks.dokkaGenerateModuleJavadoc.flatMap { it.outputDirectory })
	archiveClassifier.set("javadoc")
}

kotlin {
	// Use a specific Java version to make it easier to work in different environments.
	jvmToolchain(17)
}

publishing {
	publications {
		create<MavenPublication>("mavenJava") {
			from(components["java"])
			artifact(dokkaJavadocJar)
			pom {
				name.set("Smithy JVM")
				description.set("Converts Smithy models into Spring Boot scaffolding, generating API interfaces, DTOs, and service boilerplate to speed up backend development.")
				developers {
					developer {
						id = "justin"
						name = "Justin Wesley"
						roles = listOf("Developer")
					}
				}
				scm {
					connection = "scm:git:https://github.com/wesleyhome-com/smithy-jvm.git"
					developerConnection = "scm:git:https://github.com/wesleyhome-com/smithy-jvm.git"
					url = "https://github.com/wesleyhome-com/smithy-jvm"
					tag = "HEAD"
				}
				licenses {
					license {
						name.set("The Apache License, Version 2.0")
						url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
					}
				}
			}
		}
	}
}

signing {
	setRequired { !project.version.toString().endsWith("-SNAPSHOT") && !project.hasProperty("skipSigning") }
	if (isOnCIServer()) {
		val signingKey: String? by project
		if ((signingKey?.length ?: 0) <= 0) {
			throw RuntimeException("No Signing Key")
		}
		useInMemoryPgpKeys(signingKey, "")
	}
	sign(publishing.publications["mavenJava"])
}

tasks.withType<JavaCompile>() {
	options.encoding = "UTF-8"
}

tasks.withType<Javadoc>() {
	options.encoding = "UTF-8"

	setDestinationDir(file("$buildFile/javadoc"))
	if (JavaVersion.current().isJava9Compatible) {
		(options as StandardJavadocDocletOptions).apply {
			addBooleanOption("html5", true)
		}
	}
}

fun isOnCIServer() = System.getenv("CI") == "true"

tasks.withType<Test>().configureEach {
	// Configure all test Gradle tasks to use JUnitPlatform.
	useJUnitPlatform()

	// Log information about all test results, not only the failed ones.
	testLogging {
		events(
			TestLogEvent.FAILED,
			TestLogEvent.PASSED,
			TestLogEvent.SKIPPED
		)
	}
}
