package com.wesleyhome.smithy.generator

class SpringServerIntegration : JavaCodegenIntegration {
    override fun name(): String = "spring-server-integration"

    override fun supports(target: JavaCodegenTarget): Boolean = target == JavaCodegenTarget.SERVER

	override fun generatorContributions(context: JavaCodegenContext): List<GeneratorContribution> {
        return listOf(
	        GeneratorContribution(
                family = JavaGeneratorFamilies.SERVER_STRUCTURES,
                generators = listOf(JavaStructureGenerator(context))
            ),
	        GeneratorContribution(
                family = JavaGeneratorFamilies.SERVER_EXCEPTIONS,
                generators = listOf(JavaExceptionGenerator(context))
            ),
	        GeneratorContribution(
                family = JavaGeneratorFamilies.SERVER_ENUMS,
                generators = listOf(JavaEnumGenerator(context))
            ),
	        GeneratorContribution(
                family = JavaGeneratorFamilies.SERVER_UNIONS,
                generators = listOf(JavaUnionGenerator(context))
            ),
	        GeneratorContribution(
                family = JavaGeneratorFamilies.SERVER_API,
                generators = listOf(JavaSpringOperationApiGenerator())
            ),
	        GeneratorContribution(
                family = JavaGeneratorFamilies.SERVER_CONTROLLER,
                generators = listOf(JavaSpringControllerGenerator())
            ),
	        GeneratorContribution(
                family = JavaGeneratorFamilies.SERVER_EXCEPTION_HANDLER,
                generators = listOf(JavaSpringGlobalExceptionHandlerGenerator())
            ),
	        GeneratorContribution(
                family = JavaGeneratorFamilies.SERVER_FALLBACK_CONFIG,
                generators = listOf(JavaSpringFallbackConfigGenerator())
            )
        )
    }
}
