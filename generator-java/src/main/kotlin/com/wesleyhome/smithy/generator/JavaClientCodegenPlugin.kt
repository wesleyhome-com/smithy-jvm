package com.wesleyhome.smithy.generator

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
        val result = JavaCodegenRunner.run(
            context = context,
            target = JavaCodegenTarget.CLIENT,
            integrations = listOf(ClientIntegration())
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
