package com.example.smithy.generator

import assertk.assertThat
import assertk.assertions.contains
import org.junit.jupiter.api.Test
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.shapes.ShapeId
import software.amazon.smithy.model.shapes.ServiceShape

class JavaSpringGlobalExceptionHandlerGeneratorTest {

    @Test
    fun `generates exception handler returning clean dto`() {
        val model = Model.assembler()
            .addUnparsedModel("test.smithy", """
                namespace com.example
                
                service MyService {
                    operations: [MyOperation]
                }
                
                operation MyOperation {
                    errors: [NotFoundError]
                }
                
                @error("client")
                @httpError(404)
                structure NotFoundError {
                    @required
                    message: String
                }
            """.trimIndent())
            .assemble()
            .unwrap()
        
        val serviceId = ShapeId.from("com.example#MyService")
        val service = model.expectShape(serviceId, ServiceShape::class.java)
        val symbolProvider = JavaSymbolProvider(model, "com.example.generated")
        
        val generator = JavaSpringGlobalExceptionHandlerGenerator()
        val result = generator.generate(service, model, symbolProvider)
        val code = result.files.first().content
        
        assertThat(code).contains("@ControllerAdvice")
        assertThat(code).contains("public class GlobalExceptionHandler")
        assertThat(code).contains("@ExceptionHandler(NotFoundError.class)")
        assertThat(code).contains("public ResponseEntity<Object> handleNotFoundError(NotFoundError ex)")
        assertThat(code).contains("return new ResponseEntity<>(ex.toDto(), HttpStatus.valueOf(404));")
    }
}
