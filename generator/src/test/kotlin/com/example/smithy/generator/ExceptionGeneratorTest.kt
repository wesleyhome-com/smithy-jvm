package com.example.smithy.generator

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.doesNotContain
import org.junit.jupiter.api.Test
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.shapes.ShapeId
import software.amazon.smithy.model.shapes.StructureShape

class ExceptionGeneratorTest {
@Test
fun `generates java exception without ResponseStatus`() {
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

    val generator = ExceptionGenerator(model, symbolProvider, shape)
    val javaFile = generator.generate()
    val code = javaFile.toString()

    assertThat(code).contains("public class NotFoundError extends RuntimeException")
    assertThat(code).contains("super(message)")
    assertThat(code).contains("public static record Data(@JsonProperty(\"message\") String message) {")
    assertThat(code).contains("public Data toDto() {")
    assertThat(code).contains("return new Data(getMessage());")
}

@Test
fun `generates java exception with extra fields and DTO`() {
    val model = Model.assembler()
        .addUnparsedModel("test.smithy", """
            namespace com.example

            @error("client")
            @httpError(400)
            structure InvalidInputError {
                @required
                message: String
                
                reason: String
            }
        """.trimIndent())
        .assemble()
        .unwrap()

    val shapeId = ShapeId.from("com.example#InvalidInputError")
    val shape = model.expectShape(shapeId, StructureShape::class.java)
    val symbolProvider = JavaSymbolProvider(model, "com.example.generated")

    val generator = ExceptionGenerator(model, symbolProvider, shape)
    val javaFile = generator.generate()
    val code = javaFile.toString()

    assertThat(code).contains("public class InvalidInputError extends RuntimeException")
    assertThat(code).contains("private final String reason;")
    assertThat(code).contains("public InvalidInputError(String message, String reason)")
    assertThat(code).contains("super(message);")
    assertThat(code).contains("this.reason = reason;")
    assertThat(code).contains("public static record Data(@JsonProperty(\"message\") String message,\n      @JsonProperty(\"reason\") String reason) {")
    assertThat(code).contains("public Data toDto() {")
    assertThat(code).contains("return new Data(getMessage(), reason);")
}
}
