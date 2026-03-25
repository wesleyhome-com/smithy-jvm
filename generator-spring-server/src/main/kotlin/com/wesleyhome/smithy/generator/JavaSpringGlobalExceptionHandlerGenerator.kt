package com.wesleyhome.smithy.generator

import com.palantir.javapoet.*
import software.amazon.smithy.codegen.core.SymbolProvider
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.knowledge.TopDownIndex
import software.amazon.smithy.model.shapes.OperationShape
import software.amazon.smithy.model.shapes.ServiceShape
import software.amazon.smithy.model.shapes.StructureShape
import software.amazon.smithy.model.traits.ErrorTrait
import software.amazon.smithy.model.traits.HttpErrorTrait
import javax.lang.model.element.Modifier

/**
 * Generates a Spring Boot @ControllerAdvice for handling exceptions across a Smithy service.
 */
class JavaSpringGlobalExceptionHandlerGenerator : ShapeGenerator<ServiceShape> {
    override val shapeType: Class<ServiceShape> = ServiceShape::class.java

    override fun generate(shape: ServiceShape, model: Model, symbolProvider: SymbolProvider): ShapeGenerator.Result {
        // Find all unique error shapes used by the service's operations
        val topDownIndex = TopDownIndex.of(model)
        val operations = topDownIndex.getContainedOperations(shape)
        
        val errorShapes = operations.asSequence()
            .flatMap { it.getErrors() }
            .distinct()
            .map { model.expectShape(it, StructureShape::class.java) }
            .toList()

        if (errorShapes.isEmpty()) {
            return ShapeGenerator.Result()
        }

        val serviceSymbol = symbolProvider.toSymbol(shape)
        val className = "GlobalExceptionHandler"
        // We assume the handler goes in the same package as the controllers or base package
        val packageName = "${serviceSymbol.namespace}.controller"

        val typeBuilder = TypeSpec.classBuilder(className)
            .addModifiers(Modifier.PUBLIC)
            .addAnnotation(ClassName.get("org.springframework.web.bind.annotation", "ControllerAdvice"))

        val responseEntity = ClassName.get("org.springframework.http", "ResponseEntity")
        val httpStatus = ClassName.get("org.springframework.http", "HttpStatus")
        val objectClass = ClassName.get("java.lang", "Object")

        for (errorShape in errorShapes) {
            val symbol = symbolProvider.toSymbol(errorShape)
            val exceptionClass = ClassName.get(symbol.namespace, symbol.name)
            val methodName = "handle${symbol.name}"
            
            val errorCode = errorShape.getTrait(HttpErrorTrait::class.java)
                .map { it.code }
                .orElse(500)

            val methodBuilder = MethodSpec.methodBuilder(methodName)
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(AnnotationSpec.builder(ClassName.get("org.springframework.web.bind.annotation", "ExceptionHandler"))
                    .addMember("value", "\$T.class", exceptionClass)
                    .build())
                .returns(ParameterizedTypeName.get(responseEntity, objectClass))
                .addParameter(exceptionClass, "ex")
                
            // Build the response entity returning the exception's data DTO
            methodBuilder.addStatement("return new \$T<>(ex.toDto(), \$T.valueOf(\$L))", responseEntity, httpStatus, errorCode)
            
            typeBuilder.addMethod(methodBuilder.build())
        }

        val javaFile = JavaFile.builder(packageName, typeBuilder.build()).build()
        return ShapeGenerator.Result(listOf(javaFile.toGeneratedFile()))
    }
}
