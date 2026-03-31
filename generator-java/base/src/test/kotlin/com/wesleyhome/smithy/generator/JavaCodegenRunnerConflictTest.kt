package com.wesleyhome.smithy.generator

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.isFalse
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import software.amazon.smithy.build.MockManifest
import software.amazon.smithy.build.PluginContext
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.node.Node
import software.amazon.smithy.model.shapes.ServiceShape
import software.amazon.smithy.model.shapes.Shape
import java.nio.file.Paths

class JavaCodegenRunnerConflictTest {

	@Test
	fun `fails when two integrations claim same family at same priority`() {
		val model = Model.assembler()
			.addUnparsedModel(
				"test.smithy", """
                namespace com.wesleyhome
                service TestService {
                    version: "1.0"
                }
            """.trimIndent()
			)
			.assemble()
			.unwrap()

		val context = PluginContext.builder()
			.model(model)
			.fileManifest(MockManifest(Paths.get(".")))
			.settings(
				Node.objectNodeBuilder()
					.withMember("service", "com.wesleyhome#TestService")
					.build()
			)
			.build()

		val ex = assertThrows<IllegalStateException> {
			JavaCodegenRunner.run(
				context = context,
				target = CodegenTarget.MODEL,
				integrations = listOf(ConflictIntegrationA(), ConflictIntegrationB())
			)
		}

		assertThat(ex.message ?: "").contains("Multiple integrations claim family 'test:conflict'")
	}

	@Test
	fun `fails when two client transport integrations claim same family at same priority`() {
		val context = createPluginContext()

		val ex = assertThrows<IllegalStateException> {
			JavaCodegenRunner.run(
				context = context,
				target = CodegenTarget.CLIENT,
				integrations = listOf(
					ClientFamilyIntegration(
						name = "client-jdk-a",
						family = GeneratorFamilies.CLIENT_HTTP_TRANSPORT_JDK,
						priority = 10
					),
					ClientFamilyIntegration(
						name = "client-jdk-b",
						family = GeneratorFamilies.CLIENT_HTTP_TRANSPORT_JDK,
						priority = 10
					)
				)
			)
		}

		assertThat(
			ex.message ?: ""
		).contains("Multiple integrations claim family '${GeneratorFamilies.CLIENT_HTTP_TRANSPORT_JDK}'")
	}

	@Test
	fun `chooses highest priority winner for client codec family`() {
		val context = createPluginContext()

		val result = JavaCodegenRunner.run(
			context = context,
			target = CodegenTarget.CLIENT,
			integrations = listOf(
				ClientFamilyIntegration(
					name = "codec-low",
					family = GeneratorFamilies.CLIENT_PROTOCOL_CODEC_GSON,
					priority = 3,
					marker = "low"
				),
				ClientFamilyIntegration(
					name = "codec-high",
					family = GeneratorFamilies.CLIENT_PROTOCOL_CODEC_GSON,
					priority = 9,
					marker = "high"
				)
			)
		)

		val paths = result.files.map { it.path }
		assertThat(paths).contains("test/marker-high.txt")
		assertThat(paths.contains("test/marker-low.txt")).isFalse()
	}

	private fun createPluginContext(): PluginContext {
		val model = Model.assembler()
			.addUnparsedModel(
				"test.smithy", """
                namespace com.wesleyhome
                service TestService {
                    version: "1.0"
                }
            """.trimIndent()
			)
			.assemble()
			.unwrap()

		return PluginContext.builder()
			.model(model)
			.fileManifest(MockManifest(Paths.get(".")))
			.settings(
				Node.objectNodeBuilder()
					.withMember("service", "com.wesleyhome#TestService")
					.build()
			)
			.build()
	}

	private class ConflictIntegrationA : JavaCodegenIntegration {
		override fun name(): String = "conflict-a"

		override fun priority(): Byte = 5

		override fun supports(target: CodegenTarget): Boolean = target == CodegenTarget.MODEL

		override fun generatorContributions(context: JavaCodegenContext): List<GeneratorContribution> {
			return listOf(
				GeneratorContribution(
					family = "test:conflict",
					generators = listOf(NoOpShapeGenerator())
				)
			)
		}
	}

	private class ConflictIntegrationB : JavaCodegenIntegration {
		override fun name(): String = "conflict-b"

		override fun priority(): Byte = 5

		override fun supports(target: CodegenTarget): Boolean = target == CodegenTarget.MODEL

		override fun generatorContributions(context: JavaCodegenContext): List<GeneratorContribution> {
			return listOf(
				GeneratorContribution(
					family = "test:conflict",
					generators = listOf(NoOpShapeGenerator())
				)
			)
		}
	}

	private class NoOpShapeGenerator : ShapeGenerator<Shape> {
		override val shapeType: Class<Shape> = Shape::class.java

		override fun generate(
			shape: Shape,
			model: Model,
			symbolProvider: software.amazon.smithy.codegen.core.SymbolProvider
		): ShapeGenerator.Result = ShapeGenerator.Result()
	}

	private class MarkerServiceGenerator(private val marker: String) : ShapeGenerator<ServiceShape> {
		override val shapeType: Class<ServiceShape> = ServiceShape::class.java

		override fun generate(
			shape: ServiceShape,
			model: Model,
			symbolProvider: software.amazon.smithy.codegen.core.SymbolProvider
		): ShapeGenerator.Result = ShapeGenerator.Result(
			files = listOf(GeneratedFile(path = "test/marker-$marker.txt", content = marker))
		)
	}

	private class ClientFamilyIntegration(
		private val name: String,
		private val family: String,
		private val priority: Byte,
		private val marker: String = name
	) : JavaCodegenIntegration {
		override fun name(): String = name

		override fun priority(): Byte = priority

		override fun supports(target: CodegenTarget): Boolean = target == CodegenTarget.CLIENT

		override fun generatorContributions(context: JavaCodegenContext): List<GeneratorContribution> {
			return listOf(
				GeneratorContribution(
					family = family,
					generators = listOf(MarkerServiceGenerator(marker))
				)
			)
		}
	}
}
