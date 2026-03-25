package com.wesleyhome.smithy.generator

import software.amazon.smithy.build.PluginContext
import software.amazon.smithy.codegen.core.ReservedWordSymbolProvider
import software.amazon.smithy.codegen.core.SymbolProvider
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.neighbor.Walker
import software.amazon.smithy.model.shapes.ServiceShape
import software.amazon.smithy.model.shapes.Shape
import software.amazon.smithy.model.shapes.ShapeId
import software.amazon.smithy.model.transform.ModelTransformer
import software.amazon.smithy.model.validation.ValidationEvent

/**
 * Reusable engine for executing Java-based Smithy generation strategies.
 * Orchestrates model transformation, service closure computation, and strategy execution.
 */
object JavaCodegenRunner {

    /**
     * The result of the Java codegen orchestration.
     */
    data class Result(val files: List<GeneratedFile>,
                      val validationEvents: List<ValidationEvent>,
                      val model: Model,
                      val serviceShape: ServiceShape,
                      val symbolProvider: SymbolProvider,
                      val basePackage: String
    )

    fun run(context: PluginContext, strategies: List<ShapeGenerator<out Shape>>
    ): Result {
        var model = context.model
        val settings = context.settings

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

        val basePackage = settings.getStringMember("package").map { it.value }.orElse("com.wesleyhome.generated")
        val dtoSuffix = settings.getStringMember("dtoSuffix").map { it.value }.orElse("DTO")

        // Pass the serviceShape to our SymbolProvider so it handles renames
        val baseSymbolProvider = JavaSymbolProvider(model, basePackage, dtoSuffix, serviceShape)
        val symbolProvider =
            ReservedWordSymbolProvider.builder().symbolProvider(baseSymbolProvider).nameReservedWords(JavaReservedWords)
                .memberReservedWords(JavaReservedWords).build()

        // 4. The Engine Pipeline: Iterate over shapes within the service closure
        val (generatedFiles, validationEvents) = shapeClosure
            .filter { it.id.namespace != "smithy.api" }
            .flatMap { shape ->
                strategies
                    .filter { it.shapeType.isInstance(shape) }
                    .map { strategy ->
                        @Suppress("UNCHECKED_CAST")
                        val shapeGenerator = strategy as ShapeGenerator<Shape>
                        val result = shapeGenerator.generate(shape, model, symbolProvider)
                        result.files to result.validationEvents
                    }
            }
            .unzip()
            .let { (filesLists, eventsLists) ->
                filesLists.flatten() to eventsLists.flatten()
            }

        return Result(
            files = generatedFiles,
            validationEvents = validationEvents,
            model = model,
            serviceShape = serviceShape,
            symbolProvider = symbolProvider,
            basePackage = basePackage
        )
    }
}
