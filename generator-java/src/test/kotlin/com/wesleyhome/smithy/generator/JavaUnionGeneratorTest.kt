package com.wesleyhome.smithy.generator

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.isFalse
import org.junit.jupiter.api.Test
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.node.Node
import software.amazon.smithy.model.shapes.ServiceShape
import software.amazon.smithy.model.shapes.ShapeId
import software.amazon.smithy.model.shapes.UnionShape

class JavaUnionGeneratorTest {

	@Test
	fun `generates java sealed interface for union`() {
		val model = Model.assembler().addUnparsedModel(
			"test.smithy", """
                namespace com.wesleyhome
                union MyUnion {
                    stringValue: String,
                    intValue: Integer
                }
            """.trimIndent()
		).assemble().unwrap()

		val shapeId = ShapeId.from("com.wesleyhome#MyUnion")
		val shape = model.expectShape(shapeId, UnionShape::class.java)
		val symbolProvider = JavaSymbolProvider(model, "com.wesleyhome.generated")
		val context = createContext(model, symbolProvider, "jackson")

		val generator = JavaUnionGenerator(context)
		val generatedFiles = generator.generate(shape, model, symbolProvider)
		val code = generatedFiles.files.first().content

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

	@Test
	fun `respects serializationLibrary setting`() {
		val model = Model.assembler().addUnparsedModel(
			"test.smithy", """
                namespace com.wesleyhome
                union MyUnion {
                    foo: String
                }
            """.trimIndent()
		).assemble().unwrap()

		val shapeId = ShapeId.from("com.wesleyhome#MyUnion")
		val shape = model.expectShape(shapeId, UnionShape::class.java)
		val symbolProvider = JavaSymbolProvider(model, "com.wesleyhome.generated")

		// Case 1: Jackson (Default)
		val jacksonContext = createContext(model, symbolProvider, "jackson")
		val jacksonCode =
			JavaUnionGenerator(jacksonContext).generate(shape, model, symbolProvider).files.first().content
		assertThat(jacksonCode).contains("@JsonTypeInfo")
		assertThat(jacksonCode).contains("@JsonSubTypes")

		// Case 2: None
		val noneContext = createContext(model, symbolProvider, "none")
		val noneCode = JavaUnionGenerator(noneContext).generate(shape, model, symbolProvider).files.first().content
		assertThat(noneCode.contains("@JsonTypeInfo")).isFalse()
		assertThat(noneCode.contains("@JsonSubTypes")).isFalse()
	}

	private fun createContext(
		model: Model,
		symbolProvider: software.amazon.smithy.codegen.core.SymbolProvider,
		serializationLibrary: String
	): JavaCodegenContext {
		val serviceShape = ServiceShape.builder().id("com.wesleyhome#TestService").version("1.0").build()
		val settings = Node.objectNodeBuilder().withMember("serializationLibrary", serializationLibrary).build()
		val integrations = listOf<JavaCodegenIntegration>(JacksonIntegration())
		return JavaCodegenContext(
			model = model,
			settings = JavaSettings.from(settings),
			serviceShape = serviceShape,
			symbolProvider = symbolProvider,
			integrations = integrations,
			target = JavaCodegenTarget.MODEL
		)
	}
}
