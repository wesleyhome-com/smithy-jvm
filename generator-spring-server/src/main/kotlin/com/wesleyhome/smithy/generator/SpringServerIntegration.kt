package com.wesleyhome.smithy.generator

class SpringServerIntegration : JavaCodegenIntegration {
    override fun name(): String = "spring-server-integration"

    override fun supports(target: JavaCodegenTarget): Boolean = target == JavaCodegenTarget.SERVER

    override fun generatorContributions(context: JavaCodegenContext): List<JavaGeneratorContribution> {
        val serializationLibrary = "jackson"
        return listOf(
            JavaGeneratorContribution(
                family = JavaGeneratorFamilies.SERVER_STRUCTURES,
                generators = listOf(JavaStructureGenerator(serializationLibrary))
            ),
            JavaGeneratorContribution(
                family = JavaGeneratorFamilies.SERVER_EXCEPTIONS,
                generators = listOf(JavaExceptionGenerator(serializationLibrary))
            ),
            JavaGeneratorContribution(
                family = JavaGeneratorFamilies.SERVER_ENUMS,
                generators = listOf(JavaEnumGenerator(serializationLibrary))
            ),
            JavaGeneratorContribution(
                family = JavaGeneratorFamilies.SERVER_UNIONS,
                generators = listOf(JavaUnionGenerator(serializationLibrary))
            ),
            JavaGeneratorContribution(
                family = JavaGeneratorFamilies.SERVER_API,
                generators = listOf(JavaSpringOperationApiGenerator())
            ),
            JavaGeneratorContribution(
                family = JavaGeneratorFamilies.SERVER_CONTROLLER,
                generators = listOf(JavaSpringControllerGenerator())
            ),
            JavaGeneratorContribution(
                family = JavaGeneratorFamilies.SERVER_EXCEPTION_HANDLER,
                generators = listOf(JavaSpringGlobalExceptionHandlerGenerator())
            ),
            JavaGeneratorContribution(
                family = JavaGeneratorFamilies.SERVER_FALLBACK_CONFIG,
                generators = listOf(JavaSpringFallbackConfigGenerator())
            )
        )
    }
}
