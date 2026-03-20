package com.example.smithy.generator

import assertk.assertThat
import assertk.assertions.contains
import org.junit.jupiter.api.Test
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.shapes.ShapeId
import software.amazon.smithy.model.shapes.StructureShape

class GlobalExceptionHandlerGeneratorTest {

    @Test
    fun `generates exception handler returning clean dto`() {
        val model = Model.assembler()
            .addUnparsedModel("test.smithy", """
                namespace com.example
                
                @error("client")
                @httpError(404)
                structure NotFoundError {
                    @required
                    message: String
                }
            """.trimIndent())
            .assemble()
            .unwrap()
        
        val shapeId = ShapeId.from("com.example#NotFoundError")
        val shape = model.expectShape(shapeId, StructureShape::class.java)
        val symbolProvider = JavaSymbolProvider(model, "com.example.generated")
        
        val generator = GlobalExceptionHandlerGenerator(model, symbolProvider, "com.example.generated.config")
        val javaFile = generator.generate(listOf(shape))
        val code = javaFile.toString()
        
        assertThat(code).contains("@ControllerAdvice")
        assertThat(code).contains("public class GlobalExceptionHandler")
        assertThat(code).contains("@ExceptionHandler(NotFoundError.class)")
        assertThat(code).contains("public ResponseEntity<Object> handleNotFoundError(NotFoundError ex)")
        assertThat(code).contains("return new ResponseEntity<>(ex.toDto(), HttpStatus.valueOf(404));")
    }
}
