package com.wesleyhome.smithy.generator

import software.amazon.smithy.model.shapes.Shape

class ClientIntegration : JavaCodegenIntegration {
	override fun name(): String = "client-integration"

	override fun supports(target: JavaCodegenTarget): Boolean = target == JavaCodegenTarget.CLIENT

	override fun additionalShapeGenerators(context: JavaCodegenContext): List<ShapeGenerator<out Shape>> {
		val serializationLibrary = context.settings.getString("serializationLibrary") ?: "none"
		val httpClientLibrary = context.settings.getString("httpClientLibrary") ?: "jdk"
		return listOf(
			JavaStructureGenerator(serializationLibrary),
			JavaExceptionGenerator(serializationLibrary),
			JavaEnumGenerator(serializationLibrary),
			JavaUnionGenerator(serializationLibrary),
			JavaClientCoreAbstractionsGenerator(serializationLibrary, httpClientLibrary),
			JavaClientGenerator(serializationLibrary, httpClientLibrary)
		)
	}
}
