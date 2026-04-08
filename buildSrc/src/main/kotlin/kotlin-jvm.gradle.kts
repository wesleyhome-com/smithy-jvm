// The code in this file is a convention plugin - a Gradle mechanism for sharing reusable build logic.
// `buildSrc` is a Gradle-recognized directory and every plugin there will be easily available in the rest of the build.
package buildsrc.convention

import org.gradle.api.tasks.testing.logging.TestLogEvent

plugins {
	// Apply the Kotlin JVM plugin to add support for Kotlin in JVM projects.
	kotlin("jvm")
	`java-library`
	id("com.vanniktech.maven.publish")
	id("org.jetbrains.dokka-javadoc")
}

val versionString = providers.gradleProperty("version").get()
version = versionString

repositories {
	// Use Maven Central for resolving dependencies.
	mavenLocal()
	mavenCentral()
}

kotlin {
	// Use a specific Java version to make it easier to work in different environments.
	jvmToolchain(17)
}

mavenPublishing {
	publishToMavenCentral(automaticRelease = true)
	signAllPublications()
	pom {
		name.set("Smithy JVM")
		description.set("Converts Smithy models into Spring Boot scaffolding, generating API interfaces, DTOs, and service boilerplate to speed up backend development.")
		url.set("https://github.com/wesleyhome-com/smithy-jvm")
		developers {
			developer {
				id.set("justin")
				name.set("Justin Wesley")
			}
		}
		scm {
			connection.set("scm:git:https://github.com/wesleyhome-com/smithy-jvm.git")
			developerConnection.set("scm:git:https://github.com/wesleyhome-com/smithy-jvm.git")
			url.set("https://github.com/wesleyhome-com/smithy-jvm")
		}
		licenses {
			license {
				name.set("The Apache License, Version 2.0")
				url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
			}
		}
	}
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
