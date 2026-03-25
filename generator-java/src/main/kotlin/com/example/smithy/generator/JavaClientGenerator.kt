package com.example.smithy.generator

import com.palantir.javapoet.*
import software.amazon.smithy.codegen.core.SymbolProvider
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.shapes.ServiceShape
import software.amazon.smithy.model.shapes.OperationShape
import software.amazon.smithy.model.knowledge.TopDownIndex
import javax.lang.model.element.Modifier

/**
 * A simple strategy to generate a Java Client for a Smithy Service.
 */
class JavaClientGenerator : ShapeGenerator<ServiceShape> {
    override val shapeType: Class<ServiceShape> = ServiceShape::class.java

    override fun generate(shape: ServiceShape, model: Model, symbolProvider: SymbolProvider): ShapeGenerator.Result {
        val serviceSymbol = symbolProvider.toSymbol(shape)
        val clientName = "${serviceSymbol.name}Client"
        val packageName = "${serviceSymbol.namespace}.client"

        val typeBuilder = TypeSpec.classBuilder(clientName)
            .addModifiers(Modifier.PUBLIC)
            .addJavadoc("Lightweight client for ${shape.id.name}\n")

        val topDownIndex = TopDownIndex.of(model)
        val operations = topDownIndex.getContainedOperations(shape)

        for (operation in operations) {
            val operationName = operation.id.name.lowercase()
            val methodBuilder = MethodSpec.methodBuilder(operationName)
                .addModifiers(Modifier.PUBLIC)
                .addComment("TODO: Implement HTTP call for ${operation.id.name}")
            
            typeBuilder.addMethod(methodBuilder.build())
        }

        val javaFile = JavaFile.builder(packageName, typeBuilder.build()).build()
        return ShapeGenerator.Result(listOf(javaFile.toGeneratedFile()))
    }
}
