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
import java.util.ServiceLoader

/**
 * Reusable engine for executing Java-based Smithy generation strategies.
 * Orchestrates model transformation, service closure computation, and strategy execution.
 */
object JavaCodegenRunner {
    private data class ContributionCandidate(
        val integration: JavaCodegenIntegration,
        val contribution: JavaGeneratorContribution
    )

    /**
     * The result of the Java codegen orchestration.
     */
    data class Result(
        val files: List<GeneratedFile>,
        val validationEvents: List<ValidationEvent>,
        val model: Model,
        val serviceShape: ServiceShape,
        val symbolProvider: SymbolProvider,
        val basePackage: String
    )

    fun run(context: PluginContext, strategies: List<ShapeGenerator<out Shape>>): Result {
        return run(
            context = context,
            target = JavaCodegenTarget.MODEL,
            integrations = listOf(LegacyStrategyIntegration(strategies))
        )
    }

    fun run(
        context: PluginContext,
        target: JavaCodegenTarget,
        integrations: List<JavaCodegenIntegration> = emptyList()
    ): Result {
        val settings = JavaSettings.from(context.settings)

        // 1. Resolve Target Service
        val serviceString = settings.requireString("service")
        val serviceId = ShapeId.from(serviceString)

        // 2. Transform Model: Ensure dedicated inputs/outputs
        val transformer = ModelTransformer.create()
        var model = transformer.createDedicatedInputAndOutput(context.model, "Input", "Output")

        // Re-resolve the service shape against the newly transformed model
        var serviceShape = model.expectShape(serviceId, ServiceShape::class.java)

        // 3. Compute Service Closure
        val walker = Walker(model)
        val shapeClosure = walker.walkShapes(serviceShape)

        val basePackage = settings.getString("package") ?: "com.wesleyhome.generated"
        val dtoSuffix = settings.getString("dtoSuffix") ?: "DTO"

        var symbolProvider: SymbolProvider = ReservedWordSymbolProvider.builder()
            .symbolProvider(JavaSymbolProvider(model, basePackage, dtoSuffix, serviceShape))
            .nameReservedWords(JavaReservedWords)
            .memberReservedWords(JavaReservedWords)
            .build()

        val discoveredIntegrations = ServiceLoader.load(
            JavaCodegenIntegration::class.java,
            JavaCodegenRunner::class.java.classLoader
        ).toList()
        val activeIntegrations = (discoveredIntegrations + integrations)
            .filter { it.supports(target) }
            .sortedBy { it.priority() }

        var codegenContext = JavaCodegenContext(
            model = model,
            settings = settings,
            serviceShape = serviceShape,
            symbolProvider = symbolProvider,
            integrations = activeIntegrations,
            target = target
        )

        for (integration in activeIntegrations) {
            model = integration.preprocessModel(codegenContext)
            serviceShape = model.expectShape(serviceId, ServiceShape::class.java)
            codegenContext = codegenContext.copy(model = model, serviceShape = serviceShape)
        }

        symbolProvider = ReservedWordSymbolProvider.builder()
            .symbolProvider(JavaSymbolProvider(model, basePackage, dtoSuffix, serviceShape))
            .nameReservedWords(JavaReservedWords)
            .memberReservedWords(JavaReservedWords)
            .build()
        codegenContext = codegenContext.copy(symbolProvider = symbolProvider)

        for (integration in activeIntegrations) {
            symbolProvider = integration.decorateSymbolProvider(codegenContext, symbolProvider)
            codegenContext = codegenContext.copy(symbolProvider = symbolProvider)
        }

        val strategies = resolveStrategies(activeIntegrations, codegenContext)

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

    private fun resolveStrategies(
        integrations: List<JavaCodegenIntegration>,
        context: JavaCodegenContext
    ): List<ShapeGenerator<out Shape>> {
        val candidatesByFamily = integrations
            .flatMap { integration ->
                integration.generatorContributions(context).map { contribution ->
                    ContributionCandidate(integration = integration, contribution = contribution)
                }
            }
            .groupBy { it.contribution.family }

        return candidatesByFamily.values
            .map { familyCandidates ->
                val maxPriority = familyCandidates.maxOf { it.integration.priority() }
                val winners = familyCandidates.filter { it.integration.priority() == maxPriority }
                if (winners.size > 1) {
                    val family = familyCandidates.first().contribution.family
                    val names = winners.joinToString(", ") { it.integration.name() }
                    throw IllegalStateException(
                        "Multiple integrations claim family '$family' at priority $maxPriority: $names"
                    )
                }
                winners.single().contribution.generators
            }
            .flatten()
    }
}
