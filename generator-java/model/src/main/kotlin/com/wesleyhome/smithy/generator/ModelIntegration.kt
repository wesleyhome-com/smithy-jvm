package com.wesleyhome.smithy.generator

class ModelIntegration : JavaCodegenIntegration {
	override fun name(): String = "model-integration"

	override fun supports(target: CodegenTarget): Boolean = target == CodegenTarget.MODEL

	override fun generatorContributions(context: JavaCodegenContext): List<GeneratorContribution> {
		return listOf(
			GeneratorContribution(
				family = GeneratorFamilies.MODEL_STRUCTURES,
				generators = listOf(JavaStructureGenerator(context))
			),
			GeneratorContribution(
				family = GeneratorFamilies.MODEL_EXCEPTIONS,
				generators = listOf(JavaExceptionGenerator(codegenContext = context))
			),
			GeneratorContribution(
				family = GeneratorFamilies.MODEL_ENUMS,
				generators = listOf(JavaEnumGenerator(context))
			),
			GeneratorContribution(
				family = GeneratorFamilies.MODEL_UNIONS,
				generators = listOf(JavaUnionGenerator(context))
			)
		)
	}
}
