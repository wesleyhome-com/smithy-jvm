package com.wesleyhome.smithy.generator

import software.amazon.smithy.codegen.core.SymbolProvider
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.shapes.Shape
import software.amazon.smithy.model.validation.ValidationEvent

/**
 * Defines a strategy for converting a specific Smithy Shape into one or more source files.
 */
interface ShapeGenerator<T : Shape> {

    /**
     * The specific Shape class this generator handles (e.g., StructureShape::class.java).
     */
    val shapeType: Class<T>

    /**
     * Executes the generation logic. Returns a result containing generated files and validation events.
     */
    fun generate(shape: T, model: Model, symbolProvider: SymbolProvider): Result

	/**
	 * Runtime dispatch entrypoint for callers holding [ShapeGenerator] as an erased type.
	 */
	fun generateUntyped(shape: Shape, model: Model, symbolProvider: SymbolProvider): Result {
		@Suppress("UNCHECKED_CAST")
		return generate(shape as T, model, symbolProvider)
	}

    /**
     * The result of a generation execution.
     */
    data class Result(
        val files: List<GeneratedFile> = emptyList(),
        val validationEvents: List<ValidationEvent> = emptyList()
    )
}
