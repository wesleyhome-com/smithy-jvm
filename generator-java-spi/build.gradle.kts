plugins {
    id("buildsrc.convention.kotlin-jvm")
    `java-library`
}

dependencies {
    api(project(":generator-core"))
    api(libs.smithy.codegenCore)
    api(libs.smithy.model)
    api(libs.javapoet)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}
