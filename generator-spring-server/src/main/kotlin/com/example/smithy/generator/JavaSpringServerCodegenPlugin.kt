package com.example.smithy.generator

import java.util.logging.Logger
import software.amazon.smithy.build.PluginContext
import software.amazon.smithy.build.SmithyBuildPlugin
import software.amazon.smithy.codegen.core.ReservedWordSymbolProvider
import software.amazon.smithy.codegen.core.SymbolProvider
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.shapes.ServiceShape
import software.amazon.smithy.model.shapes.Shape
import software.amazon.smithy.model.shapes.ShapeId
import software.amazon.smithy.model.neighbor.Walker
import software.amazon.smithy.model.transform.ModelTransformer
import software.amazon.smithy.model.validation.ValidatedResultException
import software.amazon.smithy.model.validation.ValidationEvent

/**
 * A Smithy build plugin that generates Spring Boot 3/4 Java code from a Smithy model.
 */
class JavaSpringServerCodegenPlugin : SmithyBuildPlugin {
    private val LOGGER = Logger.getLogger(JavaSpringServerCodegenPlugin::class.java.name)

    override fun getName(): String {
        return "java-spring-server"
    }

    override fun execute(context: PluginContext) {
        LOGGER.info("Executing Spring Delegate Generator Plugin")
        val settings = context.settings
        val manifest = context.fileManifest

        val useResponseEntity = settings.getBooleanMemberOrDefault("useResponseEntity", true)
        val serializationLibrary = "jackson"

        // 1. Run the orchestration with all strategies (Models + Spring Server)
        val result = JavaCodegenRunner.run(
            context = context,
            strategies = listOf(
                JavaStructureGenerator(serializationLibrary),
                JavaExceptionGenerator(),
                JavaEnumGenerator(serializationLibrary),
                JavaUnionGenerator(serializationLibrary),
                JavaSpringOperationApiGenerator(useResponseEntity),
                JavaSpringControllerGenerator(useResponseEntity),
                JavaSpringGlobalExceptionHandlerGenerator(),
                JavaSpringFallbackConfigGenerator()
            )
        )

        // 2. Validation Phase
        if (result.validationEvents.any { it.severity == software.amazon.smithy.model.validation.Severity.ERROR }) {
            throw ValidatedResultException(result.validationEvents)
        }

        // 3. Commit Phase: Write files to disk
        for (file in result.files) {
            manifest.writeFile(file.path, file.content)
        }

        // 4. Post-processing: Special handling for Spring configuration imports
        val configClassName = "SpringDelegateFallbackConfiguration"
        val configPackage = "${result.basePackage}.config"
        val importsPath = "META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports"
        val fullClassName = "$configPackage.$configClassName"
        
        // We write the imports only if we actually generated the fallback config.
        if (result.files.any { it.path.endsWith("$configClassName.java") }) {
            manifest.writeFile(importsPath, fullClassName + "\n")
        }
    }
}
