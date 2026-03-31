package com.wesleyhome.smithy.generator

import software.amazon.smithy.model.shapes.Shape

/**
 * A set of generators contributed for a logical family. Only one integration wins per family.
 */
data class GeneratorContribution(
	val family: String,
	val generators: List<ShapeGenerator<out Shape>>
)
