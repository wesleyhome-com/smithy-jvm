plugins {
	java
	id("software.amazon.smithy.gradle.smithy-base") version "1.4.0"
}

group = "com.wesleyhome"
version = "0.0.1-SNAPSHOT"

java {
	sourceCompatibility = JavaVersion.VERSION_25
}

repositories {
	mavenLocal()
	mavenCentral()
}

dependencies {
	smithyBuild(project(":generator-java-client"))
}

val generatedDirectory = layout.buildDirectory.get().dir("generated/sources")
val smithyOutput: Directory = generatedDirectory.dir("smithy")

smithy {
	outputDirectory.set(smithyOutput)
}

sourceSets {
	main {
		java {
			srcDir(smithyOutput.dir("client/java-client/"))
		}
		resources {
			srcDir("model")
		}
	}
}

tasks.processResources {
	dependsOn("smithyBuild")
}

tasks.compileJava {
	dependsOn("smithyBuild")
}
