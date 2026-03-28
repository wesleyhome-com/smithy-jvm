package com.wesleyhome.smithy.generator

import software.amazon.smithy.model.shapes.Shape

/**
 * Adapter that exposes legacy hardcoded strategy lists through the integration SPI.
 */
class LegacyStrategyIntegration(
    private val strategies: List<ShapeGenerator<out Shape>>,
    private val targets: Set<JavaCodegenTarget> = JavaCodegenTarget.entries.toSet()
) : JavaCodegenIntegration {

    constructor(strategies: List<ShapeGenerator<out Shape>>, target: JavaCodegenTarget) : this(
        strategies = strategies,
        targets = setOf(target)
    )

    override fun name(): String = "legacy-strategies"

    override fun supports(target: JavaCodegenTarget): Boolean = target in targets

    override fun additionalShapeGenerators(context: JavaCodegenContext): List<ShapeGenerator<out Shape>> = strategies
}
