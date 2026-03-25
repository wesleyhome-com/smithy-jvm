package com.wesleyhome.smithy.generator

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.isEqualTo
import org.junit.jupiter.api.Test
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.shapes.ShapeId
import software.amazon.smithy.model.shapes.ServiceShape

class JavaSpringOperationApiGeneratorTest {

    @Test
    fun `generates operation api interface and stub`() {
        val model = Model.assembler()
            .addUnparsedModel("test.smithy", """
                namespace com.wesleyhome
                service MyService {
                    version: "2023-01-01",
                    operations: [SayHello]
                }
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
        
        val serviceId = ShapeId.from("com.wesleyhome#MyService")
        val service = model.expectShape(serviceId, ServiceShape::class.java)
        val symbolProvider = JavaSymbolProvider(model, "com.wesleyhome.generated")
        
        val generator = JavaSpringOperationApiGenerator()
        val generatedFiles = generator.generate(service, model, symbolProvider)
        
        // We expect 2 files per operation (Interface + Stub)
        assertThat(generatedFiles.files.size).isEqualTo(2)
        
        val interfaceCode = generatedFiles.files[0].content
        val stubCode = generatedFiles.files[1].content
        
        // Check Interface
        assertThat(interfaceCode).contains("public interface SayHelloApi")
        
        // Check Stub
        assertThat(stubCode).contains("public class SayHelloApiStub implements SayHelloApi")
        assertThat(stubCode).contains("throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED")
    }

    @Test
    fun `generates operation api with javadoc`() {
        val model = Model.assembler()
            .addUnparsedModel("test.smithy", """
                namespace com.wesleyhome
                service MyService {
                    version: "2023-01-01",
                    operations: [SayHello]
                }
                @documentation("Say hello to someone.")
                operation SayHello {
                    input: SayHelloInput
                }
                
                structure SayHelloInput {
                    @documentation("The name of the person to greet.")
                    @httpPayload
                    name: String
                }
            """.trimIndent())
            .assemble()
            .unwrap()
        
        val serviceId = ShapeId.from("com.wesleyhome#MyService")
        val service = model.expectShape(serviceId, ServiceShape::class.java)
        val symbolProvider = JavaSymbolProvider(model, "com.wesleyhome.generated")
        
        val generator = JavaSpringOperationApiGenerator()
        val generatedFiles = generator.generate(service, model, symbolProvider)
        
        val interfaceCode = generatedFiles.files[0].content
        
        // Check Method Signature (No ResponseEntity in API anymore)
        assertThat(interfaceCode).contains("void sayHello(@Valid String name);")
    }
}
