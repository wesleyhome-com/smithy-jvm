plugins {
	id("buildsrc.convention.kotlin-jvm")
}

dependencies {
	implementation(project(":generator-java-spi"))
	implementation(project(":generator-java"))
}

java {
	sourceCompatibility = JavaVersion.VERSION_17
	targetCompatibility = JavaVersion.VERSION_17
}
