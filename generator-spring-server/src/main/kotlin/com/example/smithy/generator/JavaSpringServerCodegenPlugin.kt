package com.example.smithy.generator

import java.util.logging.Logger
import software.amazon.smithy.build.PluginContext
import software.amazon.smithy.build.SmithyBuildPlugin
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.shapes.Shape
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
        val model = context.model
        val settings = context.settings
        val manifest = context.fileManifest

        val basePackage = settings.getStringMember("package").map { it.value }.orElse("com.example.generated")
        val useResponseEntity = settings.getBooleanMemberOrDefault("useResponseEntity", true)
        val dtoSuffix = settings.getStringMember("dtoSuffix").map { it.value }.orElse("DTO")
        val symbolProvider = JavaSymbolProvider(model, basePackage, dtoSuffix)

        // 1. Register active strategies for this plugin execution
        val strategies = listOf(
            JavaStructureGenerator(),
            JavaExceptionGenerator(),
            JavaEnumGenerator(),
            JavaUnionGenerator(),
            JavaSpringOperationApiGenerator(useResponseEntity),
            JavaSpringControllerGenerator(useResponseEntity),
            JavaSpringGlobalExceptionHandlerGenerator(),
            JavaSpringFallbackConfigGenerator()
        )

        val validationEvents = mutableListOf<ValidationEvent>()
        val generatedFiles = mutableListOf<GeneratedFile>()

        // 2. The Engine Pipeline: Iterate over every shape in the model
        model.shapes().forEach { shape ->
            // Skip Smithy internal shapes
            if (shape.id.namespace == "smithy.api") return@forEach

            strategies.forEach { strategy ->
                if (strategy.shapeType.isInstance(shape)) {
                    @Suppress("UNCHECKED_CAST")
                    val specificShape = shape as Shape // This cast is safe because strategy.shapeType is a subclass of Shape
                    
                    // Bridge the generic gap safely
                    val result = generateSafely(strategy as ShapeGenerator<Shape>, shape, model, symbolProvider)
                    
                    generatedFiles.addAll(result.files)
                    validationEvents.addAll(result.validationEvents)
                }
            }
        }

        // 3. Validation Phase
        if (validationEvents.any { it.severity == software.amazon.smithy.model.validation.Severity.ERROR }) {
            throw ValidatedResultException(validationEvents)
        }

        // 4. Commit Phase: Write files to disk
        for (file in generatedFiles) {
            manifest.writeFile(file.path, file.content)
        }

        // Special handling for Spring configuration imports (if a fallback config was generated)
        val configClassName = "SpringDelegateFallbackConfiguration"
        // We assume the config package is consistent with JavaSpringFallbackConfigGenerator
        // In a real project, this might be better managed by a specific strategy or shared metadata.
        val configPackage = "$basePackage.config"
        val importsPath = "META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports"
        val fullClassName = "$configPackage.$configClassName"
        
        // We write the imports only if we generated at least one file. 
        // Note: In Phase 1, we might write this even if no service existed. 
        // To be safe, let's only write it if we actually generated the fallback config.
        if (generatedFiles.any { it.path.endsWith("$configClassName.java") }) {
            manifest.writeFile(importsPath, fullClassName + "\n")
        }
    }

    private fun <T : Shape> generateSafely(
        strategy: ShapeGenerator<T>,
        shape: Shape,
        model: Model,
        symbolProvider: JavaSymbolProvider
    ): ShapeGenerator.Result {
        return strategy.generate(shape as T, model, symbolProvider)
    }
}
