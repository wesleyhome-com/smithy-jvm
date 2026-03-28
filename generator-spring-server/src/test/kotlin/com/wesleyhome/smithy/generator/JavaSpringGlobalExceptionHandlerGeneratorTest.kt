package com.wesleyhome.smithy.generator

import assertk.assertThat
import assertk.assertions.contains
import org.junit.jupiter.api.Test
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.shapes.ServiceShape
import software.amazon.smithy.model.shapes.ShapeId

class JavaSpringGlobalExceptionHandlerGeneratorTest {

    @Test
    fun `generates exception handler returning clean dto`() {
        val model = Model.assembler()
            .addUnparsedModel(
                "test.smithy", """
                namespace com.wesleyhome
                
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
            """.trimIndent()
            )
            .assemble()
            .unwrap()

        val serviceId = ShapeId.from("com.wesleyhome#MyService")
        val service = model.expectShape(serviceId, ServiceShape::class.java)
        val symbolProvider = JavaSymbolProvider(model, "com.wesleyhome.generated")

        val generator = JavaSpringGlobalExceptionHandlerGenerator()
        val result = generator.generate(service, model, symbolProvider)
        val code = result.files.first().content

        assertThat(code).contains("@ControllerAdvice")
        assertThat(code).contains("public class GlobalExceptionHandler")
        assertThat(code).contains("@ExceptionHandler(NotFoundError.class)")
        assertThat(code).contains("public ResponseEntity<Object> handleNotFoundError(NotFoundError ex)")
        assertThat(code).contains("return new ResponseEntity<>(ex.toDto(), HttpStatus.valueOf(404));")
    }

    @Test
    fun `generates exception handler for errors on resource operations`() {
        val model = Model.assembler()
            .addUnparsedModel(
                "test.smithy", """
                namespace com.wesleyhome
                
                service MyService {
                    resources: [MyResource]
                }
                
                resource MyResource {
                    identifiers: { id: String }
                    read: GetResource
                }
                
                @readonly
                operation GetResource {
                    input: GetResourceInput,
                    output: GetResourceOutput,
                    errors: [NotFoundError]
                }
                
                structure GetResourceInput {
                    @required
                    @httpLabel
                    id: String
                }
                
                structure GetResourceOutput {
                    name: String
                }
                
                @error("client")
                @httpError(404)
                structure NotFoundError {
                    @required
                    message: String
                }
            """.trimIndent()
            )
            .assemble()
            .unwrap()

        val serviceId = ShapeId.from("com.wesleyhome#MyService")
        val service = model.expectShape(serviceId, ServiceShape::class.java)
        val symbolProvider = JavaSymbolProvider(model, "com.wesleyhome.generated")

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
