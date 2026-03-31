package com.wesleyhome.smithy.generator

class SpringServerIntegration : JavaCodegenIntegration {
    override fun name(): String = "spring-server-integration"

	override fun supports(target: CodegenTarget): Boolean = target == CodegenTarget.SERVER

	override fun generatorContributions(context: JavaCodegenContext): List<GeneratorContribution> {
        return listOf(
	        GeneratorContribution(
		        family = GeneratorFamilies.SERVER_STRUCTURES,
                generators = listOf(JavaStructureGenerator(context))
            ),
	        GeneratorContribution(
		        family = GeneratorFamilies.SERVER_EXCEPTIONS,
                generators = listOf(JavaExceptionGenerator(context))
            ),
	        GeneratorContribution(
		        family = GeneratorFamilies.SERVER_ENUMS,
                generators = listOf(JavaEnumGenerator(context))
            ),
	        GeneratorContribution(
		        family = GeneratorFamilies.SERVER_UNIONS,
                generators = listOf(JavaUnionGenerator(context))
            ),
	        GeneratorContribution(
		        family = GeneratorFamilies.SERVER_API,
                generators = listOf(JavaSpringOperationApiGenerator())
            ),
	        GeneratorContribution(
		        family = GeneratorFamilies.SERVER_CONTROLLER,
                generators = listOf(JavaSpringControllerGenerator())
            ),
	        GeneratorContribution(
		        family = GeneratorFamilies.SERVER_EXCEPTION_HANDLER,
                generators = listOf(JavaSpringGlobalExceptionHandlerGenerator())
            ),
	        GeneratorContribution(
		        family = GeneratorFamilies.SERVER_FALLBACK_CONFIG,
                generators = listOf(JavaSpringFallbackConfigGenerator())
            )
        )
    }
}
