package com.example.smithy.generator

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.isFalse
import org.junit.jupiter.api.Test
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.shapes.EnumShape
import software.amazon.smithy.model.shapes.ShapeId
import software.amazon.smithy.model.shapes.StringShape

class JavaEnumGeneratorTest {

    @Test
    fun `generates java enum with safe fallback for smithy 1_0 string enum`() {
        val model = Model.assembler()
            .addUnparsedModel("test.smithy", """
                namespace com.example
                
                @documentation("The status of a thing.")
                @enum([
                    { value: "active", name: "ACTIVE", documentation: "It is active." },
                    { value: "inactive", name: "INACTIVE" }
                ])
                string Status
            """.trimIndent())
            .assemble()
            .unwrap()
        
        val shapeId = ShapeId.from("com.example#Status")
        val shape = model.expectShape(shapeId, StringShape::class.java)
        val symbolProvider = JavaSymbolProvider(model, "com.example.generated")
        
        val generator = JavaEnumGenerator()
        val generatedFiles = generator.generate(shape, model, symbolProvider)
        val code = generatedFiles.files.first().content
        
        // Check basic enum structure
        assertThat(code).contains("public enum StatusDTO")
        assertThat(code).contains("ACTIVE(\"active\")")
        assertThat(code).contains("INACTIVE(\"inactive\")")
        
        // Check javadoc
        assertThat(code).contains("/**")
        assertThat(code).contains("* The status of a thing.")
        assertThat(code).contains("   * It is active.")
        
        // Check fallback and Jackson annotations
        assertThat(code).contains("UNKNOWN_TO_SDK_VERSION(\"UNKNOWN_TO_SDK_VERSION\")")
        assertThat(code).contains("@JsonEnumDefaultValue")
        assertThat(code).contains("@JsonValue")
        assertThat(code).contains("@JsonCreator")
        
        // Check fromValue logic
        assertThat(code).contains("return UNKNOWN_TO_SDK_VERSION;")
    }

    @Test
    fun `generates java enum with safe fallback for smithy 2_0 enum shape`() {
        val model = Model.assembler()
            .addUnparsedModel("test.smithy", """
                ${'$'}version: "2"
                namespace com.example
                
                @documentation("The state of a process.")
                enum ProcessState {
                    @documentation("The process is starting.")
                    STARTING = "starting"
                    RUNNING = "running"
                    STOPPED = "stopped"
                }
            """.trimIndent())
            .assemble()
            .unwrap()
        
        val shapeId = ShapeId.from("com.example#ProcessState")
        val shape = model.expectShape(shapeId, EnumShape::class.java)
        val symbolProvider = JavaSymbolProvider(model, "com.example.generated")
        
        val generator = JavaEnumGenerator()
        val generatedFiles = generator.generate(shape, model, symbolProvider)
        val code = generatedFiles.files.first().content
        
        // Check basic enum structure
        assertThat(code).contains("public enum ProcessStateDTO")
        assertThat(code).contains("STARTING(\"starting\")")
        assertThat(code).contains("RUNNING(\"running\")")
        assertThat(code).contains("STOPPED(\"stopped\")")
        
        // Check javadoc
        assertThat(code).contains("/**")
        assertThat(code).contains("* The state of a process.")
        assertThat(code).contains("   * The process is starting.")
        
        // Check fallback and Jackson annotations
        assertThat(code).contains("UNKNOWN_TO_SDK_VERSION(\"UNKNOWN_TO_SDK_VERSION\")")
        assertThat(code).contains("@JsonEnumDefaultValue")
        assertThat(code).contains("@JsonValue")
        assertThat(code).contains("@JsonCreator")
        
        // Check fromValue logic
        assertThat(code).contains("return UNKNOWN_TO_SDK_VERSION;")
    }

    @Test
    fun `generates java enum with safe fallback for smithy 2_0 intEnum shape`() {
        val model = Model.assembler()
            .addUnparsedModel("test.smithy", """
                ${'$'}version: "2"
                namespace com.example
                
                @documentation("An integer-based status.")
                intEnum IntStatus {
                    OK = 1
                    WARNING = 2
                    CRITICAL = 3
                }
            """.trimIndent())
            .assemble()
            .unwrap()
        
        val shapeId = ShapeId.from("com.example#IntStatus")
        val shape = model.expectShape(shapeId, software.amazon.smithy.model.shapes.IntEnumShape::class.java)
        val symbolProvider = JavaSymbolProvider(model, "com.example.generated")
        
        val generator = JavaEnumGenerator()
        val generatedFiles = generator.generate(shape, model, symbolProvider)
        val code = generatedFiles.files.first().content
        
        // Check basic enum structure
        assertThat(code).contains("public enum IntStatusDTO")
        assertThat(code).contains("OK(1)")
        assertThat(code).contains("WARNING(2)")
        assertThat(code).contains("CRITICAL(3)")
        
        // Check fallback and Jackson annotations
        assertThat(code).contains("UNKNOWN_TO_SDK_VERSION(null)")
        assertThat(code).contains("@JsonEnumDefaultValue")
        assertThat(code).contains("@JsonValue")
        assertThat(code).contains("@JsonCreator")
        
        // Check value field
        assertThat(code).contains("private final Integer value;")
        assertThat(code).contains("public Integer getValue()")
        
        // Check fromValue logic
        assertThat(code).contains("return UNKNOWN_TO_SDK_VERSION;")
    }

    @Test
    fun `respects serializationLibrary setting`() {
        val model = Model.assembler()
            .addUnparsedModel("test.smithy", """
                ${'$'}version: "2"
                namespace com.example
                enum Status {
                    ACTIVE = "active"
                }
            """.trimIndent())
            .assemble()
            .unwrap()
        
        val shapeId = ShapeId.from("com.example#Status")
        val shape = model.expectShape(shapeId)
        val symbolProvider = JavaSymbolProvider(model, "com.example.generated")
        
        // Case 1: Jackson (Default)
        val jacksonCode = JavaEnumGenerator("jackson").generate(shape, model, symbolProvider).files.first().content
        assertThat(jacksonCode).contains("@JsonValue")
        assertThat(jacksonCode).contains("@JsonCreator")
        
        // Case 2: None
        val noneCode = JavaEnumGenerator("none").generate(shape, model, symbolProvider).files.first().content
        assertThat(noneCode.contains("@JsonValue")).isFalse()
        assertThat(noneCode.contains("@JsonCreator")).isFalse()
    }
}
