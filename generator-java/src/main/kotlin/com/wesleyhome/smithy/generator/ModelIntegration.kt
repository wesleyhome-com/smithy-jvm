package com.wesleyhome.smithy.generator

class ModelIntegration : JavaCodegenIntegration {
    override fun name(): String = "model-integration"

    override fun supports(target: JavaCodegenTarget): Boolean = target == JavaCodegenTarget.MODEL

    override fun generatorContributions(context: JavaCodegenContext): List<JavaGeneratorContribution> {
        val serializationLibrary = context.settings.getString("serializationLibrary") ?: "jackson"
        return listOf(
            JavaGeneratorContribution(
                family = JavaGeneratorFamilies.MODEL_STRUCTURES,
                generators = listOf(JavaStructureGenerator(serializationLibrary, context))
            ),
            JavaGeneratorContribution(
                family = JavaGeneratorFamilies.MODEL_EXCEPTIONS,
                generators = listOf(JavaExceptionGenerator(codegenContext = context))
            ),
            JavaGeneratorContribution(
                family = JavaGeneratorFamilies.MODEL_ENUMS,
                generators = listOf(JavaEnumGenerator(serializationLibrary, context))
            ),
            JavaGeneratorContribution(
                family = JavaGeneratorFamilies.MODEL_UNIONS,
                generators = listOf(JavaUnionGenerator(serializationLibrary, context))
            )
        )
    }
}
