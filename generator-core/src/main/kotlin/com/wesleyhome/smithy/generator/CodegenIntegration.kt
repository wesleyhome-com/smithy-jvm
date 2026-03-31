package com.wesleyhome.smithy.generator

import software.amazon.smithy.codegen.core.SymbolProvider
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.shapes.Shape

/**
 * Generic integration lifecycle used by language-specific SPIs.
 */
interface CodegenIntegration<C : CodegenContext<T>, T : CodegenTarget> {
	fun name(): String

	fun priority(): Byte = 0

	fun supports(target: T): Boolean = true

	fun preprocessModel(context: C): Model = context.model

	fun decorateSymbolProvider(context: C, symbolProvider: SymbolProvider): SymbolProvider = symbolProvider

	fun generatorContributions(context: C): List<GeneratorContribution> {
		val generators = additionalShapeGenerators(context)
		if (generators.isEmpty()) {
			return emptyList()
		}
		return listOf(GeneratorContribution(family = "${name()}:default", generators = generators))
	}

	fun additionalShapeGenerators(context: C): List<ShapeGenerator<out Shape>> = emptyList()
}
