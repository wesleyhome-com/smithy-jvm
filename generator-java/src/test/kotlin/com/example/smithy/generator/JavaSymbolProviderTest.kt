package com.example.smithy.generator

import assertk.assertThat
import assertk.assertions.isEqualTo
import org.junit.jupiter.api.Test
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.shapes.ShapeId
import software.amazon.smithy.model.shapes.StringShape

class JavaSymbolProviderTest {

    @Test
    fun `maps string shape to java String`() {
        val shape = StringShape.builder().id("com.example#MyString").build()
        val model = Model.builder().addShape(shape).build()
        val provider = JavaSymbolProvider(model, "com.example.generated")
        
        val symbol = provider.toSymbol(shape)
        
        assertThat(symbol.name).isEqualTo("String")
        assertThat(symbol.namespace).isEqualTo("java.lang")
    }

    @Test
    fun `maps structure shape to custom java class with DTO suffix`() {
        val shapeId = ShapeId.from("com.example#MyStructure")
        val model = Model.assembler()
            .addUnparsedModel("test.smithy", """
                namespace com.example
                structure MyStructure {
                    foo: String
                }
            """.trimIndent())
            .assemble()
            .unwrap()
        
        val provider = JavaSymbolProvider(model, "com.example.generated")
        val symbol = provider.toSymbol(model.expectShape(shapeId))
        
        assertThat(symbol.name).isEqualTo("MyStructureDTO")
        assertThat(symbol.namespace).isEqualTo("com.example.generated.model")
    }

    @Test
    fun `maps timestamp shape to java Instant`() {
        val shapeId = ShapeId.from("com.example#MyTimestamp")
        val model = Model.assembler()
            .addUnparsedModel("test.smithy", "namespace com.example\ntimestamp MyTimestamp")
            .assemble()
            .unwrap()
        
        val provider = JavaSymbolProvider(model, "com.example.generated")
        val symbol = provider.toSymbol(model.expectShape(shapeId))
        
        assertThat(symbol.name).isEqualTo("Instant")
        assertThat(symbol.namespace).isEqualTo("java.time")
    }

    @Test
    fun `maps document shape to java String`() {
        val shapeId = ShapeId.from("com.example#MyDocument")
        val model = Model.assembler()
            .addUnparsedModel("test.smithy", "namespace com.example\ndocument MyDocument")
            .assemble()
            .unwrap()
        
        val provider = JavaSymbolProvider(model, "com.example.generated")
        val symbol = provider.toSymbol(model.expectShape(shapeId))
        
        assertThat(symbol.name).isEqualTo("String")
        assertThat(symbol.namespace).isEqualTo("java.lang")
    }
}
