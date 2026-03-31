package com.wesleyhome.smithy.generator

import assertk.assertThat
import assertk.assertions.contains
import org.junit.jupiter.api.Test
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.node.Node
import software.amazon.smithy.model.shapes.ServiceShape
import software.amazon.smithy.model.shapes.ShapeId
import software.amazon.smithy.model.shapes.StructureShape

class JavaExceptionGeneratorTest {
	@Test
	fun `generates java exception without ResponseStatus`() {
		val model = Model.assembler()
			.addUnparsedModel(
				"test.smithy", """
                namespace com.wesleyhome

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

		val shapeId = ShapeId.from("com.wesleyhome#NotFoundError")
		val shape = model.expectShape(shapeId, StructureShape::class.java)
		val symbolProvider = JavaSymbolProvider(model, "com.wesleyhome.generated")
		val context = createContext(model, symbolProvider)

		val generator = JavaExceptionGenerator(context)
		val generatedFiles = generator.generate(shape, model, symbolProvider)
		val code = generatedFiles.files.first().content

		assertThat(code).contains("public class NotFoundError extends RuntimeException")
		assertThat(code).contains("super(message)")
		assertThat(code).contains("public static record Data(@JsonProperty(\"message\") String message) {")
		assertThat(code).contains("public Data toDto() {")
		assertThat(code).contains("return new Data(getMessage());")
	}

	@Test
	fun `generates java exception with extra fields and DTO`() {
		val model = Model.assembler()
			.addUnparsedModel(
				"test.smithy", """
                namespace com.wesleyhome

                @error("client")
                @httpError(400)
                structure InvalidInputError {
                    @required
                    message: String
                    
                    reason: String
                }
            """.trimIndent()
			)
			.assemble()
			.unwrap()

		val shapeId = ShapeId.from("com.wesleyhome#InvalidInputError")
		val shape = model.expectShape(shapeId, StructureShape::class.java)
		val symbolProvider = JavaSymbolProvider(model, "com.wesleyhome.generated")
		val context = createContext(model, symbolProvider)

		val generator = JavaExceptionGenerator(context)
		val generatedFiles = generator.generate(shape, model, symbolProvider)
		val code = generatedFiles.files.first().content

		assertThat(code).contains("public class InvalidInputError extends RuntimeException")
		assertThat(code).contains("private final String reason;")
		assertThat(code).contains("public InvalidInputError(String message, String reason)")
		assertThat(code).contains("super(message);")
		assertThat(code).contains("this.reason = reason;")
		assertThat(code).contains("public static record Data(@JsonProperty(\"message\") String message,\n      @JsonProperty(\"reason\") String reason) {")
		assertThat(code).contains("public Data toDto() {")
		assertThat(code).contains("return new Data(getMessage(), reason);")
	}

	private fun createContext(
		model: Model,
		symbolProvider: software.amazon.smithy.codegen.core.SymbolProvider
	): JavaCodegenContext {
		val serviceShape = ServiceShape.builder().id("com.wesleyhome#TestService").version("1.0").build()
		val settings = Node.objectNodeBuilder().build()
		val integrations = listOf<JavaCodegenIntegration>(JacksonIntegration())
		return JavaCodegenContext(
			model = model,
			settings = CodegenSettings.from(settings),
			serviceShape = serviceShape,
			symbolProvider = symbolProvider,
			integrations = integrations,
			target = CodegenTarget.MODEL
		)
	}
}
