package com.wesleyhome.smithy.generator

import assertk.assertThat
import assertk.assertions.contains
import org.junit.jupiter.api.Test
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.node.Node
import software.amazon.smithy.model.shapes.EnumShape
import software.amazon.smithy.model.shapes.ServiceShape
import software.amazon.smithy.model.shapes.ShapeId

class JavaEnumGeneratorTest {

	@Test
	fun `generates java enum with safe fallback for smithy 2_0 enum shape`() {
		val model = Model.assembler()
			.addUnparsedModel(
				"test.smithy", """
                ${'$'}version: "2"
                namespace com.wesleyhome
                
                @documentation("The state of a process.")
                enum ProcessState {
                    @documentation("The process is starting.")
                    STARTING = "starting"
                    RUNNING = "running"
                    STOPPED = "stopped"
                }
            """.trimIndent()
			)
			.assemble()
			.unwrap()

		val shapeId = ShapeId.from("com.wesleyhome#ProcessState")
		val shape = model.expectShape(shapeId, EnumShape::class.java)
		val symbolProvider = JavaSymbolProvider(model, "com.wesleyhome.generated")
		val context = createContext(model, symbolProvider)

		val generator = JavaEnumGenerator(context)
		val generatedFiles = generator.generate(shape, model, symbolProvider)
		val code = generatedFiles.files.first().content

		// Check basic enum structure
		assertThat(code).contains("public enum ProcessStateDTO")
		assertThat(code).contains("STARTING(\"starting\")")
		assertThat(code).contains("RUNNING(\"running\")")
		assertThat(code).contains("STOPPED(\"stopped\")")

		// Check javadoc
		assertThat(code).contains("/**")
		assertThat(code).contains("* The state of a process.")
		assertThat(code).contains("   * The process is starting.")

		// Check fallback and Jackson annotations
		assertThat(code).contains("UNKNOWN_TO_SDK_VERSION(\"UNKNOWN_TO_SDK_VERSION\")")
		assertThat(code).contains("@JsonEnumDefaultValue")
		assertThat(code).contains("@JsonValue")
		assertThat(code).contains("@JsonCreator")

		// Check fromValue logic
		assertThat(code).contains("return UNKNOWN_TO_SDK_VERSION;")
	}

	@Test
	fun `generates java enum with safe fallback for smithy 2_0 intEnum shape`() {
		val model = Model.assembler()
			.addUnparsedModel(
				"test.smithy", """
                ${'$'}version: "2"
                namespace com.wesleyhome
                
                @documentation("An integer-based status.")
                intEnum IntStatus {
                    OK = 1
                    WARNING = 2
                    CRITICAL = 3
                }
            """.trimIndent()
			)
			.assemble()
			.unwrap()

		val shapeId = ShapeId.from("com.wesleyhome#IntStatus")
		val shape = model.expectShape(shapeId, software.amazon.smithy.model.shapes.IntEnumShape::class.java)
		val symbolProvider = JavaSymbolProvider(model, "com.wesleyhome.generated")
		val context = createContext(model, symbolProvider)

		val generator = JavaEnumGenerator(context)
		val generatedFiles = generator.generate(shape, model, symbolProvider)
		val code = generatedFiles.files.first().content

		// Check basic enum structure
		assertThat(code).contains("public enum IntStatusDTO")
		assertThat(code).contains("OK(1)")
		assertThat(code).contains("WARNING(2)")
		assertThat(code).contains("CRITICAL(3)")

		// Check fallback and Jackson annotations
		assertThat(code).contains("UNKNOWN_TO_SDK_VERSION(null)")
		assertThat(code).contains("@JsonEnumDefaultValue")
		assertThat(code).contains("@JsonValue")
		assertThat(code).contains("@JsonCreator")

		// Check value field
		assertThat(code).contains("private final Integer value;")
		assertThat(code).contains("public Integer getValue()")

		// Check fromValue logic
		assertThat(code).contains("return UNKNOWN_TO_SDK_VERSION;")
	}

	@Test
	fun `applies jackson enum annotations when integration is present`() {
		val model = Model.assembler()
			.addUnparsedModel(
				"test.smithy", """
                ${'$'}version: "2"
                namespace com.wesleyhome
                enum Status {
                    ACTIVE = "active"
                }
            """.trimIndent()
			)
			.assemble()
			.unwrap()

		val shapeId = ShapeId.from("com.wesleyhome#Status")
		val shape = model.expectShape(shapeId)
		val symbolProvider = JavaSymbolProvider(model, "com.wesleyhome.generated")

		val jacksonContext = createContext(model, symbolProvider)
		val jacksonCode = JavaEnumGenerator(jacksonContext).generate(shape, model, symbolProvider).files.first().content
		assertThat(jacksonCode).contains("@JsonValue")
		assertThat(jacksonCode).contains("@JsonCreator")

		val context = createContext(model, symbolProvider)
		val code = JavaEnumGenerator(context).generate(shape, model, symbolProvider).files.first().content
		assertThat(code).contains("@JsonValue")
		assertThat(code).contains("@JsonCreator")
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
			settings = JavaSettings.from(settings),
			serviceShape = serviceShape,
			symbolProvider = symbolProvider,
			integrations = integrations,
			target = JavaCodegenTarget.MODEL
		)
	}
}
