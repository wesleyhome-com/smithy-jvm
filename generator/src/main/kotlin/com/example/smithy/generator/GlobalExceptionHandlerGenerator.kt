package com.example.smithy.generator

import com.palantir.javapoet.*
import software.amazon.smithy.codegen.core.SymbolProvider
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.shapes.StructureShape
import software.amazon.smithy.model.traits.ErrorTrait
import software.amazon.smithy.model.traits.HttpErrorTrait
import javax.lang.model.element.Modifier

class GlobalExceptionHandlerGenerator(
    private val model: Model,
    private val symbolProvider: SymbolProvider,
    private val basePackage: String
) {
    fun generate(errorShapes: List<StructureShape>): JavaFile {
        val className = "GlobalExceptionHandler"
        val typeBuilder = TypeSpec.classBuilder(className)
            .addModifiers(Modifier.PUBLIC)
            .addAnnotation(ClassName.get("org.springframework.web.bind.annotation", "ControllerAdvice"))

        val responseEntity = ClassName.get("org.springframework.http", "ResponseEntity")
        val httpStatus = ClassName.get("org.springframework.http", "HttpStatus")
        val objectClass = ClassName.get("java.lang", "Object")

        for (shape in errorShapes) {
            val symbol = symbolProvider.toSymbol(shape)
            val exceptionClass = ClassName.get(symbol.namespace, symbol.name)
            val methodName = "handle${symbol.name}"
            
            val errorCode = shape.getTrait(HttpErrorTrait::class.java)
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

        return JavaFile.builder(basePackage, typeBuilder.build()).build()
    }
}
