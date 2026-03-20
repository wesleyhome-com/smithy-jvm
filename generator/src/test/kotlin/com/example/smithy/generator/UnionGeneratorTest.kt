package com.example.smithy.generator

import assertk.assertThat
import assertk.assertions.contains
import org.junit.jupiter.api.Test
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.shapes.ShapeId
import software.amazon.smithy.model.shapes.UnionShape

class UnionGeneratorTest {

    @Test
    fun `generates java sealed interface for union`() {
        val model = Model.assembler().addUnparsedModel(
                "test.smithy", """
                namespace com.example
                union MyUnion {
                    stringValue: String,
                    intValue: Integer
                }
            """.trimIndent()
            ).assemble().unwrap()

        val shapeId = ShapeId.from("com.example#MyUnion")
        val shape = model.expectShape(shapeId, UnionShape::class.java)
        val symbolProvider = JavaSymbolProvider(model, "com.example.generated")

        val generator = UnionGenerator(model, symbolProvider, shape)
        val javaFile = generator.generate()
        val code = javaFile.toString()

        assertThat(code).contains("public sealed interface MyUnionDTO")
        assertThat(code).contains("@JsonTypeInfo")
        assertThat(code).contains("include = JsonTypeInfo.As.WRAPPER_OBJECT")
        assertThat(code).contains("defaultImpl = MyUnionDTO.Unknown.class")
        assertThat(code).contains("@JsonSubTypes")
        assertThat(code).contains("name = \"stringValue\"")
        assertThat(code).contains("name = \"intValue\"")

        assertThat(code).contains("final record StringValue(String value) implements MyUnionDTO")
        assertThat(code).contains("final record IntValue(Integer value) implements MyUnionDTO")
        assertThat(code).contains("final class Unknown implements MyUnionDTO")
    }
}
