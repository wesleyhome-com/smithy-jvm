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
        var model = context.model
        val settings = context.settings
        val manifest = context.fileManifest

        // 1. Resolve Target Service
        val serviceString = settings.getStringMember("service").orElseThrow {
            IllegalArgumentException("Missing required 'service' configuration in smithy-build.json")
        }.value
        val serviceId = ShapeId.from(serviceString)
        var serviceShape = model.expectShape(serviceId, ServiceShape::class.java)

        // 2. Transform Model: Ensure dedicated inputs/outputs
        val transformer = ModelTransformer.create()
        model = transformer.createDedicatedInputAndOutput(model, "Input", "Output")
        
        // Re-resolve the service shape against the newly transformed model
        serviceShape = model.expectShape(serviceId, ServiceShape::class.java)

        // 3. Compute Service Closure
        val walker = Walker(model)
        val shapeClosure = walker.walkShapes(serviceShape)

        val basePackage = settings.getStringMember("package").map { it.value }.orElse("com.example.generated")
        val useResponseEntity = settings.getBooleanMemberOrDefault("useResponseEntity", true)
        val dtoSuffix = settings.getStringMember("dtoSuffix").map { it.value }.orElse("DTO")
        
        // Pass the serviceShape to our SymbolProvider so it handles renames
        val baseSymbolProvider = JavaSymbolProvider(model, basePackage, dtoSuffix, serviceShape)
        val symbolProvider = ReservedWordSymbolProvider.builder()
            .symbolProvider(baseSymbolProvider)
            .nameReservedWords(JavaReservedWords)
            .memberReservedWords(JavaReservedWords)
            .build()

        // 4. Register active strategies for this plugin execution
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

        // 5. The Engine Pipeline: Iterate over shapes within the service closure
        shapeClosure.forEach { shape ->
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

        // 6. Validation Phase
        if (validationEvents.any { it.severity == software.amazon.smithy.model.validation.Severity.ERROR }) {
            throw ValidatedResultException(validationEvents)
        }

        // 7. Commit Phase: Write files to disk
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
        if (generatedFiles.any { it.path.endsWith("$configClassName.java") }) {
            manifest.writeFile(importsPath, fullClassName + "\n")
        }
    }

    private fun <T : Shape> generateSafely(
        strategy: ShapeGenerator<T>,
        shape: Shape,
        model: Model,
        symbolProvider: SymbolProvider
    ): ShapeGenerator.Result {
        return strategy.generate(shape as T, model, symbolProvider)
    }
}
