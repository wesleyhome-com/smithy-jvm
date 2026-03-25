package com.example.smithy.generator

import software.amazon.smithy.build.PluginContext
import software.amazon.smithy.build.SmithyBuildPlugin
import software.amazon.smithy.model.validation.Severity
import software.amazon.smithy.model.validation.ValidatedResultException

/**
 * A Smithy build plugin that generates a Java Client and required models.
 */
class JavaClientCodegenPlugin : SmithyBuildPlugin {

    override fun getName(): String = "java-client"

    override fun execute(context: PluginContext) {
        val serializationLibrary = context.settings.getStringMember("serializationLibrary").map { it.value }.orElse("none")
        val httpClientLibrary = context.settings.getStringMember("httpClientLibrary").map { it.value }.orElse("jdk")

        val result = JavaCodegenRunner.run(
            context = context,
            strategies = listOf(
                JavaStructureGenerator(serializationLibrary),
                JavaExceptionGenerator(serializationLibrary),
                JavaEnumGenerator(serializationLibrary),
                JavaUnionGenerator(serializationLibrary),
                JavaClientCoreAbstractionsGenerator(serializationLibrary, httpClientLibrary),
                JavaClientGenerator(serializationLibrary, httpClientLibrary)
            )
        )

        if (result.validationEvents.any { it.severity == Severity.ERROR }) {
            throw ValidatedResultException(result.validationEvents)
        }

        val manifest = context.fileManifest
        for (file in result.files) {
            manifest.writeFile(file.path, file.content)
        }
    }
}
