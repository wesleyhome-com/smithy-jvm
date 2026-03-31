package com.wesleyhome.smithy.generator

class ModelIntegration : JavaCodegenIntegration {
    override fun name(): String = "model-integration"

    override fun supports(target: JavaCodegenTarget): Boolean = target == JavaCodegenTarget.MODEL

	override fun generatorContributions(context: JavaCodegenContext): List<GeneratorContribution> {
        return listOf(
	        GeneratorContribution(
                family = JavaGeneratorFamilies.MODEL_STRUCTURES,
                generators = listOf(JavaStructureGenerator(context))
            ),
	        GeneratorContribution(
                family = JavaGeneratorFamilies.MODEL_EXCEPTIONS,
                generators = listOf(JavaExceptionGenerator(codegenContext = context))
            ),
	        GeneratorContribution(
                family = JavaGeneratorFamilies.MODEL_ENUMS,
                generators = listOf(JavaEnumGenerator(context))
            ),
	        GeneratorContribution(
                family = JavaGeneratorFamilies.MODEL_UNIONS,
                generators = listOf(JavaUnionGenerator(context))
            )
        )
    }
}
