package com.wesleyhome.smithy.generator

import assertk.assertThat
import assertk.assertions.isEqualTo
import org.junit.jupiter.api.Test
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.shapes.ShapeId
import software.amazon.smithy.model.shapes.StringShape

class JavaSymbolProviderTest {

	@Test
	fun `maps string shape to java String`() {
		val shape = StringShape.builder().id("com.wesleyhome#MyString").build()
		val model = Model.builder().addShape(shape).build()
		val provider = JavaSymbolProvider(model, "com.wesleyhome.generated")

		val symbol = provider.toSymbol(shape)

		assertThat(symbol.name).isEqualTo("String")
		assertThat(symbol.namespace).isEqualTo("java.lang")
	}

	@Test
	fun `maps structure shape to custom java class with DTO suffix`() {
		val shapeId = ShapeId.from("com.wesleyhome#MyStructure")
		val model = Model.assembler()
			.addUnparsedModel(
				"test.smithy", """
                namespace com.wesleyhome
                structure MyStructure {
                    foo: String
                }
            """.trimIndent()
			)
			.assemble()
			.unwrap()

		val provider = JavaSymbolProvider(model, "com.wesleyhome.generated")
		val symbol = provider.toSymbol(model.expectShape(shapeId))

		assertThat(symbol.name).isEqualTo("MyStructureDTO")
		assertThat(symbol.namespace).isEqualTo("com.wesleyhome.generated.model")
	}

	@Test
	fun `maps timestamp shape to java Instant`() {
		val shapeId = ShapeId.from("com.wesleyhome#MyTimestamp")
		val model = Model.assembler()
			.addUnparsedModel("test.smithy", "namespace com.wesleyhome\ntimestamp MyTimestamp")
			.assemble()
			.unwrap()

		val provider = JavaSymbolProvider(model, "com.wesleyhome.generated")
		val symbol = provider.toSymbol(model.expectShape(shapeId))

		assertThat(symbol.name).isEqualTo("Instant")
		assertThat(symbol.namespace).isEqualTo("java.time")
	}

	@Test
	fun `maps document shape to java String`() {
		val shapeId = ShapeId.from("com.wesleyhome#MyDocument")
		val model = Model.assembler()
			.addUnparsedModel("test.smithy", "namespace com.wesleyhome\ndocument MyDocument")
			.assemble()
			.unwrap()

		val provider = JavaSymbolProvider(model, "com.wesleyhome.generated")
		val symbol = provider.toSymbol(model.expectShape(shapeId))

		assertThat(symbol.name).isEqualTo("String")
		assertThat(symbol.namespace).isEqualTo("java.lang")
	}

	@Test
	fun `maps bigInteger shape to java math BigInteger`() {
		val shapeId = ShapeId.from("com.wesleyhome#MyBigInt")
		val model = Model.assembler()
			.addUnparsedModel("test.smithy", "namespace com.wesleyhome\nbigInteger MyBigInt")
			.assemble()
			.unwrap()

		val provider = JavaSymbolProvider(model, "com.wesleyhome.generated")
		val symbol = provider.toSymbol(model.expectShape(shapeId))

		assertThat(symbol.name).isEqualTo("BigInteger")
		assertThat(symbol.namespace).isEqualTo("java.math")
	}

	@Test
	fun `maps bigDecimal shape to java math BigDecimal`() {
		val shapeId = ShapeId.from("com.wesleyhome#MyBigDecimal")
		val model = Model.assembler()
			.addUnparsedModel("test.smithy", "namespace com.wesleyhome\nbigDecimal MyBigDecimal")
			.assemble()
			.unwrap()

		val provider = JavaSymbolProvider(model, "com.wesleyhome.generated")
		val symbol = provider.toSymbol(model.expectShape(shapeId))

		assertThat(symbol.name).isEqualTo("BigDecimal")
		assertThat(symbol.namespace).isEqualTo("java.math")
	}
}
