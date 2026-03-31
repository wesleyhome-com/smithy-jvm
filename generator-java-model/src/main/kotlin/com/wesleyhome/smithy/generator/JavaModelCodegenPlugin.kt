package com.wesleyhome.smithy.generator

import software.amazon.smithy.build.PluginContext
import software.amazon.smithy.build.SmithyBuildPlugin
import software.amazon.smithy.model.validation.Severity
import software.amazon.smithy.model.validation.ValidatedResultException

/**
 * A Smithy build plugin that generates Java domain models (POJOs/Records) from a Smithy model.
 * Does not include any framework-specific code (like Spring Boot).
 */
class JavaModelCodegenPlugin : SmithyBuildPlugin {

    override fun getName(): String = "java-model"

    override fun execute(context: PluginContext) {
        val result = JavaCodegenRunner.run(
            context = context,
            target = CodegenTarget.MODEL
        )

        // 1. Validation Phase
        if (result.validationEvents.any { it.severity == Severity.ERROR }) {
            throw ValidatedResultException(result.validationEvents)
        }

        // 2. Commit Phase: Write files to disk
        val manifest = context.fileManifest
        for (file in result.files) {
            manifest.writeFile(file.path, file.content)
        }
    }
}
