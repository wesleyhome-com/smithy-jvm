package com.wesleyhome.smithy.generator

import software.amazon.smithy.model.shapes.Shape

class SpringServerIntegration : JavaCodegenIntegration {
	override fun name(): String = "spring-server-integration"

	override fun supports(target: JavaCodegenTarget): Boolean = target == JavaCodegenTarget.SERVER

	override fun additionalShapeGenerators(context: JavaCodegenContext): List<ShapeGenerator<out Shape>> {
		val serializationLibrary = "jackson"
		return listOf(
			JavaStructureGenerator(serializationLibrary),
			JavaExceptionGenerator(serializationLibrary),
			JavaEnumGenerator(serializationLibrary),
			JavaUnionGenerator(serializationLibrary),
			JavaSpringOperationApiGenerator(),
			JavaSpringControllerGenerator(),
			JavaSpringGlobalExceptionHandlerGenerator(),
			JavaSpringFallbackConfigGenerator()
		)
	}
}
