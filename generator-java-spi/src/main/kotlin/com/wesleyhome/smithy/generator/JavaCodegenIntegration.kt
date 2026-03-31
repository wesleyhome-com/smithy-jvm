package com.wesleyhome.smithy.generator

import software.amazon.smithy.codegen.core.SymbolProvider
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.shapes.Shape

/**
 * Extension point for Java codegen customizations.
 *
 * Integrations are discovered via ServiceLoader, filtered by target, and ordered by [priority].
 * Generator-producing integrations should contribute logical families through [generatorContributions].
 *
 * Hook methods receive mutable JavaPoet builders. Implementations are expected to mutate the provided
 * builder instance in place.
 */
interface JavaCodegenIntegration {
    fun name(): String

    fun priority(): Byte = 0

    fun supports(target: CodegenTarget): Boolean = true

    fun preprocessModel(context: JavaCodegenContext): Model = context.model

    fun decorateSymbolProvider(
        context: JavaCodegenContext,
        symbolProvider: SymbolProvider
    ): SymbolProvider = symbolProvider

    /**
     * Contributes generators grouped by logical family. Runner picks one winner per family by priority.
     */
    fun generatorContributions(context: JavaCodegenContext): List<GeneratorContribution> {
        val generators = additionalShapeGenerators(context)
        if (generators.isEmpty()) {
            return emptyList()
        }
        return listOf(GeneratorContribution(family = "${name()}:default", generators = generators))
    }

    /**
     * Legacy convenience hook. Prefer [generatorContributions].
     */
    fun additionalShapeGenerators(context: JavaCodegenContext): List<ShapeGenerator<out Shape>> = emptyList()
}
