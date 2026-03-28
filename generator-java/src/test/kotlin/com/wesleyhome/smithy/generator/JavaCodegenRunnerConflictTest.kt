package com.wesleyhome.smithy.generator

import assertk.assertThat
import assertk.assertions.contains
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import software.amazon.smithy.build.MockManifest
import software.amazon.smithy.build.PluginContext
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.node.Node
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
				target = JavaCodegenTarget.MODEL,
				integrations = listOf(ConflictIntegrationA(), ConflictIntegrationB())
			)
		}

		assertThat(ex.message ?: "").contains("Multiple integrations claim family 'test:conflict'")
	}

	private class ConflictIntegrationA : JavaCodegenIntegration {
		override fun name(): String = "conflict-a"

		override fun priority(): Byte = 5

		override fun supports(target: JavaCodegenTarget): Boolean = target == JavaCodegenTarget.MODEL

		override fun generatorContributions(context: JavaCodegenContext): List<JavaGeneratorContribution> {
			return listOf(
				JavaGeneratorContribution(
					family = "test:conflict",
					generators = listOf(NoOpShapeGenerator())
				)
			)
		}
	}

	private class ConflictIntegrationB : JavaCodegenIntegration {
		override fun name(): String = "conflict-b"

		override fun priority(): Byte = 5

		override fun supports(target: JavaCodegenTarget): Boolean = target == JavaCodegenTarget.MODEL

		override fun generatorContributions(context: JavaCodegenContext): List<JavaGeneratorContribution> {
			return listOf(
				JavaGeneratorContribution(
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
}
