package com.wesleyhome.smithy.generator

class ClientIntegration : JavaCodegenIntegration {
	override fun name(): String = "client-integration"

	override fun supports(target: CodegenTarget): Boolean = target == CodegenTarget.CLIENT

	override fun generatorContributions(context: JavaCodegenContext): List<GeneratorContribution> {
		return listOf(
			GeneratorContribution(
				family = GeneratorFamilies.CLIENT_STRUCTURES,
				generators = listOf(JavaStructureGenerator(context))
			),
			GeneratorContribution(
				family = GeneratorFamilies.CLIENT_EXCEPTIONS,
				generators = listOf(JavaExceptionGenerator(context))
			),
			GeneratorContribution(
				family = GeneratorFamilies.CLIENT_ENUMS,
				generators = listOf(JavaEnumGenerator(context))
			),
			GeneratorContribution(
				family = GeneratorFamilies.CLIENT_UNIONS,
				generators = listOf(JavaUnionGenerator(context))
			),
			GeneratorContribution(
				family = GeneratorFamilies.CLIENT_CORE,
				generators = listOf(JavaClientCoreAbstractionsGenerator())
			),
			GeneratorContribution(
				family = GeneratorFamilies.CLIENT_SERVICE,
				generators = listOf(JavaClientGenerator())
			)
		)
	}
}
