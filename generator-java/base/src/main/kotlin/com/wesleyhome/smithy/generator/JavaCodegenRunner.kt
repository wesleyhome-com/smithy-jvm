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
 * Orchestrates model transformation, integration lifecycle, and strategy execution.
 */
object JavaCodegenRunner {
    private data class ContributionCandidate(
        val integration: JavaCodegenIntegration,
        val contribution: GeneratorContribution
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

    fun run(
        context: PluginContext,
        target: CodegenTarget,
        integrations: List<JavaCodegenIntegration> = emptyList()
    ): Result {
        val settings = CodegenSettings.from(context.settings)

        // 1. Resolve Target Service
        val serviceString = settings.requireString("service")
        val serviceId = ShapeId.from(serviceString)

        // 2. Transform Model: Ensure dedicated inputs/outputs
        val transformedModel = ModelTransformer.create().createDedicatedInputAndOutput(context.model, "Input", "Output")
        val transformedService = transformedModel.expectShape(serviceId, ServiceShape::class.java)

        val basePackage = settings.getString("package") ?: "com.wesleyhome.generated"
        val dtoSuffix = settings.getString("dtoSuffix") ?: "DTO"

        val discoveredIntegrations = ServiceLoader.load(
            JavaCodegenIntegration::class.java,
            JavaCodegenRunner::class.java.classLoader
        ).toList()
        val activeIntegrations = (discoveredIntegrations + integrations)
            .filter { it.supports(target) }
            .sortedBy { it.priority() }

        val initialContext = JavaCodegenContext(
            model = transformedModel,
            settings = settings,
            serviceShape = transformedService,
            symbolProvider = buildReservedSymbolProvider(transformedModel, transformedService, basePackage, dtoSuffix),
            integrations = activeIntegrations,
            target = target
        )

        val preprocessedContext = activeIntegrations.fold(initialContext) { state, integration ->
            val nextModel = integration.preprocessModel(state)
            val nextService = nextModel.expectShape(serviceId, ServiceShape::class.java)
            state.copy(model = nextModel, serviceShape = nextService)
        }

        val baseSymbolProvider = buildReservedSymbolProvider(
            preprocessedContext.model,
            preprocessedContext.serviceShape,
            basePackage,
            dtoSuffix
        )

        val decoratedSymbolProvider = activeIntegrations.fold(baseSymbolProvider) { current, integration ->
            integration.decorateSymbolProvider(preprocessedContext.copy(symbolProvider = current), current)
        }
        val finalContext = preprocessedContext.copy(symbolProvider = decoratedSymbolProvider)
        val strategies = resolveStrategies(activeIntegrations, finalContext)
        val shapeClosure = Walker(finalContext.model).walkShapes(finalContext.serviceShape)

        // 4. The Engine Pipeline: Iterate over shapes within the service closure
        val (generatedFiles, validationEvents) = shapeClosure
            .filter { it.id.namespace != "smithy.api" }
            .flatMap { shape ->
                strategies
                    .filter { it.shapeType.isInstance(shape) }
                    .map { strategy ->
                        val result = strategy.generateUntyped(shape, finalContext.model, finalContext.symbolProvider)
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
            model = finalContext.model,
            serviceShape = finalContext.serviceShape,
            symbolProvider = finalContext.symbolProvider,
            basePackage = basePackage
        )
    }

    private fun buildReservedSymbolProvider(
        model: Model,
        serviceShape: ServiceShape,
        basePackage: String,
        dtoSuffix: String
    ): SymbolProvider {
        return ReservedWordSymbolProvider.builder()
            .symbolProvider(JavaSymbolProvider(model, basePackage, dtoSuffix, serviceShape))
            .nameReservedWords(JavaReservedWords)
            .memberReservedWords(JavaReservedWords)
            .build()
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
