import org.jooq.meta.jaxb.MatcherRule
import org.jooq.meta.jaxb.MatcherTransformType

plugins {
    id("org.springframework.boot") version "4.0.3"
    id("io.spring.dependency-management") version "1.1.7"
    java
    // Apply Smithy Gradle plugin
    id("software.amazon.smithy.gradle.smithy-base") version "1.4.0"
    // Official JOOQ Gradle plugin
    id("org.jooq.jooq-codegen-gradle") version "3.19.1"
}

group = "com.example"
version = "0.0.1-SNAPSHOT"

java {
    sourceCompatibility = JavaVersion.VERSION_25
}

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-h2console")
    implementation("org.springframework.boot:spring-boot-starter-data-jdbc")
    implementation("org.springframework.boot:spring-boot-starter-flyway")
    implementation("org.springframework.boot:spring-boot-starter-jooq")
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    runtimeOnly("com.h2database:h2")
    testImplementation("org.springframework.boot:spring-boot-starter-data-jdbc-test")
    testImplementation("org.springframework.boot:spring-boot-starter-flyway-test")
    testImplementation("org.springframework.boot:spring-boot-starter-jooq-test")
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testImplementation("org.springframework.boot:spring-boot-starter-validation-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    // MapStruct
    implementation("org.mapstruct:mapstruct:1.6.3")
    annotationProcessor("org.mapstruct:mapstruct-processor:1.6.3")

    // Smithy model dependencies
    smithyBuild("com.example.smithy:generator:0.1.0-SNAPSHOT")
    smithyBuild("com.example.smithy:smithy-traits:0.1.0-SNAPSHOT")

    // JOOQ Code Gen dependencies
    jooqCodegen("org.jooq:jooq-meta-extensions:3.20.11")
}

jooq {
    version = "3.20.11"
    
    executions {
        create("main") {
            configuration {
                generator {
                    name = "org.jooq.codegen.JavaGenerator"
                    database {
                        name = "org.jooq.meta.extensions.ddl.DDLDatabase"
                        properties {
                            property {
                                key = "scripts"
                                value = "src/main/resources/db/migration/*.sql"
                            }
                            property {
                                key = "sort"
                                value = "semantic"
                            }
                        }
                    }
                    generate {
                        isRecords = true
                        isImmutablePojos = true
                        isFluentSetters = true
                    }
                    target {
                        packageName = "com.example.library.db"
                        directory = "build/generated/sources/jooq"
                    }
                }
            }
        }
    }
}

val smithyOutput = layout.buildDirectory.dir("generated/sources/smithy").get()

// Configure Smithy Build
smithy {
    outputDirectory.set(smithyOutput)
}

sourceSets {
    main {
        val srcPath = smithyOutput.dir("source/spring-delegate-generator/")
        resources {
            srcDir("model")
            srcDir(srcPath)
        }
        java {
            srcDir(srcPath)
            srcDir("build/generated/sources/jooq")
        }
    }
}

tasks.processResources {
    dependsOn("smithyBuild")
}

tasks.processTestResources {
    dependsOn("smithyBuild")
}

tasks.compileJava {
    dependsOn("smithyBuild", "jooqCodegen")
    // The JOOQ plugin usually hooks into the build automatically, 
    // but we ensure it runs before compilation.
}

tasks.withType<Test> {
    useJUnitPlatform()
}
