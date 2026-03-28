package com.wesleyhome.smithy.generator

import software.amazon.smithy.codegen.core.SymbolProvider
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.shapes.ServiceShape

class JavaClientJdkHttpTransportGenerator : ShapeGenerator<ServiceShape> {
	override val shapeType: Class<ServiceShape> = ServiceShape::class.java

	override fun generate(shape: ServiceShape, model: Model, symbolProvider: SymbolProvider): ShapeGenerator.Result {
		val serviceSymbol = symbolProvider.toSymbol(shape)
		val baseNamespace = "${serviceSymbol.namespace}.client"
		val file = JavaClientCoreAbstractionsGenerator.generateJdkHttpTransport(baseNamespace)
		return ShapeGenerator.Result(listOf(file.toGeneratedFile()))
	}
}
