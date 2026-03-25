import org.jooq.meta.jaxb.MatcherRule
import org.jooq.meta.jaxb.MatcherTransformType

plugins {
    id("org.springframework.boot") version "4.0.4"
    id("io.spring.dependency-management") version "1.1.7"
    java
    // Apply Smithy Gradle plugin
    id("software.amazon.smithy.gradle.smithy-base") version "1.4.0"
    // Official JOOQ Gradle plugin
    id("org.jooq.jooq-codegen-gradle") version "3.19.1"
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
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.boot:spring-boot-h2console")
    implementation("org.springframework.boot:spring-boot-starter-data-jdbc")
    implementation("org.springframework.boot:spring-boot-starter-flyway")
    implementation("org.springframework.boot:spring-boot-starter-jooq")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    runtimeOnly("com.h2database:h2")
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testImplementation("org.springframework.boot:spring-boot-starter-data-jdbc-test")
    testImplementation("org.springframework.boot:spring-boot-starter-flyway-test")
    testImplementation("org.springframework.boot:spring-boot-starter-jooq-test")
    testImplementation("org.springframework.boot:spring-boot-starter-validation-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    // MapStruct
    implementation("org.mapstruct:mapstruct:1.6.3")
    annotationProcessor("org.mapstruct:mapstruct-processor:1.6.3")

    // GSON for client projection
    implementation("com.google.code.gson:gson:2.12.1")

    // OkHttp for client projection
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // Smithy model dependencies
    smithyBuild(project(":generator-spring-server"))

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
                        packageName = "com.wesleyhome.library.db"
                        directory = "build/generated/sources/jooq"
                    }
                }
            }
        }
    }
}

val buildDirectory = layout.buildDirectory.get()
val generatedDirectory = buildDirectory.dir("generated/sources")
val smithyOutput: Directory = generatedDirectory.dir("smithy")

// Configure Smithy Build
smithy {
    outputDirectory.set(smithyOutput)
}

sourceSets {
    main {
        val serverPath = smithyOutput.dir("full_server/java-spring-server/")
        val modelPath = smithyOutput.dir("model_only/java-model/")
        val jacksonClientPath = smithyOutput.dir("client_jackson_jdk/java-client/")
        val gsonClientPath = smithyOutput.dir("client_gson_jdk/java-client/")
        val okhttpClientPath = smithyOutput.dir("client_okhttp_jackson/java-client/")
        val bareClientPath = smithyOutput.dir("client_bare/java-client/")
        
        resources {
            srcDir("model")
            srcDir(serverPath)
        }
        java {
            srcDir(serverPath)
            srcDir(modelPath)
            srcDir(jacksonClientPath)
            srcDir(gsonClientPath)
            srcDir(okhttpClientPath)
            srcDir(bareClientPath)
            srcDir(generatedDirectory.dir("jooq"))
        }
    }
    create("it") {
        compileClasspath += main.get().output + test.get().output
        runtimeClasspath += main.get().output + test.get().output
        java {
            srcDir("src/it/java")
        }
    }
}

val itImplementation: Configuration by configurations.getting {
    extendsFrom(configurations.testImplementation.get())
}
val itRuntimeOnly: Configuration by configurations.getting {
    extendsFrom(configurations.testRuntimeOnly.get())
}

val integrationTest = tasks.register<Test>("integrationTest") {
    description = "Runs integration tests."
    group = "verification"

    testClassesDirs = sourceSets["it"].output.classesDirs
    classpath = sourceSets["it"].runtimeClasspath
    mustRunAfter(tasks.test)

    useJUnitPlatform()
}

tasks.check {
    dependsOn(integrationTest)
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
