package com.example.smithy.generator

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.isEqualTo
import org.junit.jupiter.api.Test
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.shapes.ShapeId
import software.amazon.smithy.model.shapes.OperationShape
import software.amazon.smithy.model.validation.Severity
import software.amazon.smithy.model.validation.ValidationEvent

class ControllerGeneratorTest {

    @Test
    fun `generates controller with injected operation apis`() {
        val model = Model.assembler()
            .addUnparsedModel("test.smithy", """
                namespace com.example
                operation SayHello {
                    input: SayHelloInput,
                    output: SayHelloOutput
                }
                structure SayHelloInput {
                    @httpPayload
                    payload: HelloPayload
                }
                structure HelloPayload {
                    name: String
                }
                structure SayHelloOutput {
                    greeting: String
                }
            """.trimIndent())
            .assemble()
            .unwrap()
        
        val operationId = ShapeId.from("com.example#SayHello")
        val operation = model.expectShape(operationId, OperationShape::class.java)
        val symbolProvider = JavaSymbolProvider(model, "com.example.generated")
        
        val generator = ControllerGenerator(
            model, symbolProvider, "MyController", "com.example.generated.controller", listOf(operation),
            true
        )
        val (javaFile, _) = generator.generate()
        val code = javaFile.toString()
        
        assertThat(code).contains("@RestController")
        assertThat(code).contains("public class MyController")
        assertThat(code).contains("private final SayHelloApi sayHelloApi;")
        assertThat(code).contains("public MyController(SayHelloApi sayHelloApi)")
        assertThat(code).contains("return sayHelloApi.sayHello(payload);")
    }

    @Test
    fun `generates controller with request param default value`() {
        val model = Model.assembler()
            .addUnparsedModel("test.smithy", """
                ${'$'}version: "2.0"
                namespace com.example
                operation QueryOp {
                    input: QueryInput
                }
                structure QueryInput {
                    @httpQuery("page")
                    @default(0)
                    page: Integer,
                    
                    @httpQuery("q")
                    @default("test")
                    query: String
                }
            """.trimIndent())
            .assemble()
            .unwrap()
        
        val operationId = ShapeId.from("com.example#QueryOp")
        val operation = model.expectShape(operationId, OperationShape::class.java)
        val symbolProvider = JavaSymbolProvider(model, "com.example.generated")
        
        val generator = ControllerGenerator(
            model, symbolProvider, "MyController", "com.example.generated.controller", listOf(operation),
            true
        )
        val (javaFile, _) = generator.generate()
        val code = javaFile.toString()
        
        assertThat(code).contains("@RequestParam(value = \"page\", required = false, defaultValue = \"0\")")
        assertThat(code).contains("@RequestParam(value = \"q\", required = false, defaultValue = \"test\")")
    }

    @Test
    fun `reports error when multiple payload members are present`() {
        val model = Model.assembler()
            .addUnparsedModel("test.smithy", """
                namespace com.example
                operation BadPayload {
                    input: BadPayloadInput
                }
                structure BadPayloadInput {
                    field1: String,
                    field2: String
                }
            """.trimIndent())
            .assemble()
            .unwrap()
        
        val operationId = ShapeId.from("com.example#BadPayload")
        val operation = model.expectShape(operationId, OperationShape::class.java)
        val symbolProvider = JavaSymbolProvider(model, "com.example.generated")
        
        val generator = ControllerGenerator(
            model, symbolProvider, "MyController", "com.example.generated.controller", listOf(operation),
            true
        )
        val (_, events) = generator.generate()
        
        assertThat(events.size).isEqualTo(1)
        assertThat(events[0].id).isEqualTo("MultiplePayloadMembers")
        assertThat(events[0].severity).isEqualTo(Severity.ERROR)
    }
}
