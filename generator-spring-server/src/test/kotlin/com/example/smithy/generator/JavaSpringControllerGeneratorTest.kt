package com.example.smithy.generator

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.isEqualTo
import org.junit.jupiter.api.Test
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.shapes.ShapeId
import software.amazon.smithy.model.shapes.ServiceShape

class JavaSpringControllerGeneratorTest {

    @Test
    fun `generates controller with injected operation apis`() {
        val model = Model.assembler()
            .addUnparsedModel("test.smithy", """
                namespace com.example
                service MyService {
                    version: "2023-01-01",
                    operations: [SayHello]
                }
                @tags(["Hello"])
                operation SayHello {
                    input: SayHelloInput,
                    output: SayHelloOutput
                }
                structure SayHelloInput {
                    @httpPayload
                    payload: String
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
        // Now returns ResponseEntity
        assertThat(code).contains("SayHelloOutputDTO result = sayHelloApi.sayHello(payload);")
        assertThat(code).contains("return ResponseEntity.ok()")
        assertThat(code).contains(".body(result);")
    }

    @Test
    fun `generates controller with request param default value`() {
        val model = Model.assembler()
            .addUnparsedModel("test.smithy", """
                ${'$'}version: "2.0"
                namespace com.example
                
                service MyService {
                    version: "2023-01-01",
                    operations: [GetInfo]
                }

                @readonly
                @http(method: "GET", uri: "/info")
                operation GetInfo {
                    input: GetInfoInput
                }

                structure GetInfoInput {
                    @httpQuery("page")
                    @default(1)
                    page: Integer
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
        
        assertThat(code).contains("@RequestParam(value = \"page\", required = false, defaultValue = \"1\") Integer page")
    }

    @Test
    fun `reports error when multiple payload members are present`() {
        val model = Model.assembler()
            .addUnparsedModel("test.smithy", """
                namespace com.example
                service MyService {
                    version: "2023-01-01",
                    operations: [BadOp]
                }
                operation BadOp {
                    input: BadInput
                }
                structure BadInput {
                    foo: String,
                    bar: String
                }
            """.trimIndent())
            .assemble()
            .unwrap()
        
        val serviceId = ShapeId.from("com.example#MyService")
        val service = model.expectShape(serviceId, ServiceShape::class.java)
        val symbolProvider = JavaSymbolProvider(model, "com.example.generated")
        
        val generator = JavaSpringControllerGenerator()
        val result = generator.generate(service, model, symbolProvider)
        
        assertThat(result.validationEvents.any { it.id == "MultiplePayloadMembers" }).isEqualTo(true)
    }

    @Test
    fun `generates controller with operations bound to resources`() {
        val model = Model.assembler()
            .addUnparsedModel("test.smithy", """
                namespace com.example
                service MyService {
                    version: "2023-01-01",
                    resources: [MyResource]
                }
                resource MyResource {
                    identifiers: { id: String },
                    read: GetResource
                }
                @tags(["Resource"])
                @readonly
                @http(method: "GET", uri: "/resource/{id}")
                operation GetResource {
                    input: GetResourceInput,
                    output: GetResourceOutput
                }
                structure GetResourceInput {
                    @required
                    @httpLabel
                    id: String
                }
                structure GetResourceOutput {
                    name: String
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
        assertThat(code).contains("public class ResourceController")
        assertThat(code).contains("private final GetResourceApi getResourceApi;")
        assertThat(code).contains("public ResourceController(GetResourceApi getResourceApi)")
        // Now returns ResponseEntity
        assertThat(code).contains("GetResourceOutputDTO result = getResourceApi.getResource(id);")
        assertThat(code).contains("return ResponseEntity.ok()")
        assertThat(code).contains(".body(result);")
    }
}
