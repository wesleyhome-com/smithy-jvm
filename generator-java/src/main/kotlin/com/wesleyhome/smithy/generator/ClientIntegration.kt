package com.wesleyhome.smithy.generator

class ClientIntegration : JavaCodegenIntegration {
    override fun name(): String = "client-integration"

    override fun supports(target: JavaCodegenTarget): Boolean = target == JavaCodegenTarget.CLIENT

    override fun generatorContributions(context: JavaCodegenContext): List<JavaGeneratorContribution> {
        val serializationLibrary = context.settings.getString("serializationLibrary") ?: "none"
        val httpClientLibrary = context.settings.getString("httpClientLibrary") ?: "jdk"
        return listOf(
            JavaGeneratorContribution(
                family = JavaGeneratorFamilies.CLIENT_STRUCTURES,
	            generators = listOf(JavaStructureGenerator(context))
            ),
            JavaGeneratorContribution(
                family = JavaGeneratorFamilies.CLIENT_EXCEPTIONS,
                generators = listOf(JavaExceptionGenerator(serializationLibrary, context))
            ),
            JavaGeneratorContribution(
                family = JavaGeneratorFamilies.CLIENT_ENUMS,
                generators = listOf(JavaEnumGenerator(serializationLibrary, context))
            ),
            JavaGeneratorContribution(
                family = JavaGeneratorFamilies.CLIENT_UNIONS,
                generators = listOf(JavaUnionGenerator(serializationLibrary, context))
            ),
            JavaGeneratorContribution(
                family = JavaGeneratorFamilies.CLIENT_CORE,
                generators = listOf(JavaClientCoreAbstractionsGenerator(serializationLibrary, httpClientLibrary))
            ),
            JavaGeneratorContribution(
                family = JavaGeneratorFamilies.CLIENT_SERVICE,
                generators = listOf(JavaClientGenerator(serializationLibrary, httpClientLibrary))
            )
        )
    }
}
