package com.example.smithy.generator

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.isEqualTo
import org.junit.jupiter.api.Test
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.shapes.ShapeId
import software.amazon.smithy.model.shapes.OperationShape

class OperationApiGeneratorTest {

    @Test
    fun `generates operation api interface and stub`() {
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
        
        val generator = OperationApiGenerator(model, symbolProvider, operation)
        val javaFiles = generator.generate()
        
        assertThat(javaFiles.size).isEqualTo(2)
        
        val interfaceCode = javaFiles[0].toString()
        val stubCode = javaFiles[1].toString()
        
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
                namespace com.example
                
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
        
        val operationId = ShapeId.from("com.example#SayHello")
        val operation = model.expectShape(operationId, OperationShape::class.java)
        val symbolProvider = JavaSymbolProvider(model, "com.example.generated")
        
        val generator = OperationApiGenerator(model, symbolProvider, operation)
        val javaFiles = generator.generate()
        
        val interfaceCode = javaFiles[0].toString()
        
        // Check Interface Documentation
        assertThat(interfaceCode).contains("/**")
        assertThat(interfaceCode).contains("* Say hello to someone.")
        
        // Check Method Documentation
        assertThat(interfaceCode).contains("@param name The name of the person to greet.")
        assertThat(interfaceCode).contains("ResponseEntity<Void> sayHello(String name);")
    }
}
