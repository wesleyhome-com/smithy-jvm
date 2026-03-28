package com.wesleyhome.smithy.generator

import software.amazon.smithy.model.shapes.Shape

class ModelIntegration : JavaCodegenIntegration {
	override fun name(): String = "model-integration"

	override fun supports(target: JavaCodegenTarget): Boolean = target == JavaCodegenTarget.MODEL

	override fun additionalShapeGenerators(context: JavaCodegenContext): List<ShapeGenerator<out Shape>> {
		val serializationLibrary = context.settings.getString("serializationLibrary") ?: "jackson"
		return listOf(
			JavaStructureGenerator(serializationLibrary),
			JavaExceptionGenerator(),
			JavaEnumGenerator(serializationLibrary),
			JavaUnionGenerator(serializationLibrary)
		)
	}
}
