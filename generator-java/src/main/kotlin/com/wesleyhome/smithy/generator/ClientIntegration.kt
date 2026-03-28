package com.wesleyhome.smithy.generator

class ClientIntegration : JavaCodegenIntegration {
    override fun name(): String = "client-integration"

    override fun supports(target: JavaCodegenTarget): Boolean = target == JavaCodegenTarget.CLIENT

    override fun generatorContributions(context: JavaCodegenContext): List<JavaGeneratorContribution> {
        return listOf(
            JavaGeneratorContribution(
                family = JavaGeneratorFamilies.CLIENT_STRUCTURES,
	            generators = listOf(JavaStructureGenerator(context))
            ),
            JavaGeneratorContribution(
                family = JavaGeneratorFamilies.CLIENT_EXCEPTIONS,
                generators = listOf(JavaExceptionGenerator(context))
            ),
            JavaGeneratorContribution(
                family = JavaGeneratorFamilies.CLIENT_ENUMS,
                generators = listOf(JavaEnumGenerator(context))
            ),
            JavaGeneratorContribution(
                family = JavaGeneratorFamilies.CLIENT_UNIONS,
                generators = listOf(JavaUnionGenerator(context))
            ),
            JavaGeneratorContribution(
                family = JavaGeneratorFamilies.CLIENT_CORE,
                generators = listOf(JavaClientCoreAbstractionsGenerator())
            ),
            JavaGeneratorContribution(
                family = JavaGeneratorFamilies.CLIENT_SERVICE,
                generators = listOf(JavaClientGenerator())
            )
        )
    }
}
