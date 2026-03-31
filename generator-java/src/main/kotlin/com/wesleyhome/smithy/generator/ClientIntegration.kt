package com.wesleyhome.smithy.generator

class ClientIntegration : JavaCodegenIntegration {
    override fun name(): String = "client-integration"

    override fun supports(target: JavaCodegenTarget): Boolean = target == JavaCodegenTarget.CLIENT

    override fun generatorContributions(context: JavaCodegenContext): List<GeneratorContribution> {
        return listOf(
            GeneratorContribution(
                family = JavaGeneratorFamilies.CLIENT_STRUCTURES,
	            generators = listOf(JavaStructureGenerator(context))
            ),
            GeneratorContribution(
                family = JavaGeneratorFamilies.CLIENT_EXCEPTIONS,
                generators = listOf(JavaExceptionGenerator(context))
            ),
            GeneratorContribution(
                family = JavaGeneratorFamilies.CLIENT_ENUMS,
                generators = listOf(JavaEnumGenerator(context))
            ),
            GeneratorContribution(
                family = JavaGeneratorFamilies.CLIENT_UNIONS,
                generators = listOf(JavaUnionGenerator(context))
            ),
            GeneratorContribution(
                family = JavaGeneratorFamilies.CLIENT_CORE,
                generators = listOf(JavaClientCoreAbstractionsGenerator())
            ),
            GeneratorContribution(
                family = JavaGeneratorFamilies.CLIENT_SERVICE,
                generators = listOf(JavaClientGenerator())
            )
        )
    }
}
