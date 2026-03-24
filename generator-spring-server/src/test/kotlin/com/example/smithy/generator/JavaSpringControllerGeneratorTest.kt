package com.example.smithy.generator

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.isEqualTo
import org.junit.jupiter.api.Test
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.shapes.ShapeId
import software.amazon.smithy.model.shapes.ServiceShape
import software.amazon.smithy.model.validation.Severity

class JavaSpringControllerGeneratorTest {

    @Test
    fun `generates controller with injected operation apis`() {
        val model = Model.assembler()
            .addUnparsedModel("test.smithy", """
                namespace com.example
                
                service MyService {
                    operations: [SayHello]
                }
                
                @tags(["Hello"])
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
        
        val serviceId = ShapeId.from("com.example#MyService")
        val service = model.expectShape(serviceId, ServiceShape::class.java)
        val symbolProvider = JavaSymbolProvider(model, "com.example.generated")
        
        val generator = JavaSpringControllerGenerator()
        val result = generator.generate(service, model, symbolProvider)
        val code = result.files.first().content
        
        assertThat(code).contains("@RestController")
        assertThat(code).contains("public class HelloController")
        assertThat(code).contains("private final SayHelloApi sayHelloApi;")
        assertThat(code).contains("public HelloController(SayHelloApi sayHelloApi)")
        assertThat(code).contains("return sayHelloApi.sayHello(payload);")
    }

    @Test
    fun `generates controller with request param default value`() {
        val model = Model.assembler()
            .addUnparsedModel("test.smithy", """
                ${'$'}version: "2.0"
                namespace com.example
                
                service MyService {
                    operations: [QueryOp]
                }
                
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
        
        val serviceId = ShapeId.from("com.example#MyService")
        val service = model.expectShape(serviceId, ServiceShape::class.java)
        val symbolProvider = JavaSymbolProvider(model, "com.example.generated")
        
        val generator = JavaSpringControllerGenerator()
        val result = generator.generate(service, model, symbolProvider)
        val code = result.files.first().content
        
        assertThat(code).contains("@RequestParam(value = \"page\", required = false, defaultValue = \"0\")")
        assertThat(code).contains("@RequestParam(value = \"q\", required = false, defaultValue = \"test\")")
    }

    @Test
    fun `reports error when multiple payload members are present`() {
        val model = Model.assembler()
            .addUnparsedModel("test.smithy", """
                namespace com.example
                
                service MyService {
                    operations: [BadPayload]
                }
                
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
        
        val serviceId = ShapeId.from("com.example#MyService")
        val service = model.expectShape(serviceId, ServiceShape::class.java)
        val symbolProvider = JavaSymbolProvider(model, "com.example.generated")
        
        val generator = JavaSpringControllerGenerator()
        val result = generator.generate(service, model, symbolProvider)
        
        assertThat(result.validationEvents.size).isEqualTo(1)
        assertThat(result.validationEvents[0].id).isEqualTo("MultiplePayloadMembers")
        assertThat(result.validationEvents[0].severity).isEqualTo(Severity.ERROR)
    }
}
