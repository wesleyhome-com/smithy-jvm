package com.wesleyhome.smithy.generator

import software.amazon.smithy.codegen.core.SymbolProvider
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.shapes.ServiceShape

class JavaClientOkHttpTransportGenerator : ShapeGenerator<ServiceShape> {
	override val shapeType: Class<ServiceShape> = ServiceShape::class.java

	override fun generate(shape: ServiceShape, model: Model, symbolProvider: SymbolProvider): ShapeGenerator.Result {
		val serviceSymbol = symbolProvider.toSymbol(shape)
		val baseNamespace = "${serviceSymbol.namespace}.client"
		val file = JavaClientCoreAbstractionsGenerator.generateOkHttpTransport(baseNamespace)
		return ShapeGenerator.Result(listOf(file.toGeneratedFile()))
	}
}
