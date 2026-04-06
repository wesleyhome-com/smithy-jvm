package com.wesleyhome.smithy.generator

import assertk.assertThat
import assertk.assertions.isEqualTo
import org.junit.jupiter.api.Test
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.shapes.ServiceShape
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

	@Test
	fun `maps resource-bound operation and models to root domain package`() {
		val model = Model.assembler()
			.addUnparsedModel(
				"test.smithy", """
                ${'$'}version: "2"
                namespace com.wesleyhome

                service Library {
                    version: "2024-01-01"
                    resources: [Patron]
                }

                resource Patron {
                    identifiers: { id: String }
                    read: GetPatron
                    resources: [Contact]
                }

                resource Contact {
                    identifiers: { id: String }
                    update: UpdateContact
                }

                @readonly
                @http(method: "GET", uri: "/patrons/{id}")
                operation GetPatron {
                    input: GetPatronInput
                    output: GetPatronOutput
                }

                @http(method: "PATCH", uri: "/patrons/{id}/contact")
                operation UpdateContact {
                    input: UpdateContactInput
                    output: UpdateContactOutput
                }

                structure GetPatronInput {
                    @required
                    @httpLabel
                    id: String
                }

                structure GetPatronOutput {
                    id: String
                    name: String
                }

                structure UpdateContactInput {
                    @required
                    @httpLabel
                    id: String
                    @required
                    email: String
                }

                structure UpdateContactOutput { id: String }
            """.trimIndent()
			)
			.assemble()
			.unwrap()

		val service = model.expectShape(ShapeId.from("com.wesleyhome#Library"), ServiceShape::class.java)
		val provider = JavaSymbolProvider(model, "com.wesleyhome.generated", serviceShape = service)

		val updateContact = model.expectShape(ShapeId.from("com.wesleyhome#UpdateContact"))
		val updateContactInput = model.expectShape(ShapeId.from("com.wesleyhome#UpdateContactInput"))

		assertThat(provider.toSymbol(updateContact).namespace).isEqualTo("com.wesleyhome.generated.patron.api")
		assertThat(provider.toSymbol(updateContactInput).namespace).isEqualTo("com.wesleyhome.generated.patron.model")
	}

	@Test
	fun `maps unbound tagged operation to domain package via tag fallback`() {
		val model = Model.assembler()
			.addUnparsedModel(
				"test.smithy", """
                ${'$'}version: "2"
                namespace com.wesleyhome

                service Library {
                    version: "2024-01-01"
                    operations: [SearchCatalog]
                }

                @tags(["Catalog"])
                operation SearchCatalog {
                    input: SearchCatalogInput
                    output: SearchCatalogOutput
                }

                @tags(["Catalog"])
                structure SearchCatalogInput { query: String }

                @tags(["Catalog"])
                structure SearchCatalogOutput { count: Integer }
            """.trimIndent()
			)
			.assemble()
			.unwrap()

		val service = model.expectShape(ShapeId.from("com.wesleyhome#Library"), ServiceShape::class.java)
		val provider = JavaSymbolProvider(model, "com.wesleyhome.generated", serviceShape = service)

		val op = model.expectShape(ShapeId.from("com.wesleyhome#SearchCatalog"))
		val input = model.expectShape(ShapeId.from("com.wesleyhome#SearchCatalogInput"))
		assertThat(provider.toSymbol(op).namespace).isEqualTo("com.wesleyhome.generated.catalog.api")
		assertThat(provider.toSymbol(input).namespace).isEqualTo("com.wesleyhome.generated.catalog.model")
	}
}
