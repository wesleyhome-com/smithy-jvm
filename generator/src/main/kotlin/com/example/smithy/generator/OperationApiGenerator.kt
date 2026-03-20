package com.example.smithy.generator

import com.palantir.javapoet.*
import software.amazon.smithy.codegen.core.SymbolProvider
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.shapes.OperationShape
import software.amazon.smithy.model.shapes.StructureShape
import software.amazon.smithy.model.traits.DocumentationTrait
import software.amazon.smithy.utils.StringUtils
import javax.lang.model.element.Modifier

class OperationApiGenerator(
    private val model: Model,
    private val symbolProvider: SymbolProvider,
    private val operation: OperationShape,
    private val useResponseEntity: Boolean = true
) {
    fun generate(): List<JavaFile> {
        val operationSymbol = symbolProvider.toSymbol(operation)
        val interfaceName = operationSymbol.name
        val packageName = operationSymbol.namespace
        val interfaceType = ClassName.get(packageName, interfaceName)

        val interfaceFile = generateInterface(interfaceName, packageName)
        val stubFile = generateStub(interfaceName, packageName, interfaceType)

        return listOf(interfaceFile, stubFile)
    }

    private fun generateInterface(name: String, packageName: String): JavaFile {
        val typeBuilder = TypeSpec.interfaceBuilder(name)
            .addModifiers(Modifier.PUBLIC)

        operation.getTrait(DocumentationTrait::class.java).ifPresent { trait ->
            typeBuilder.addJavadoc("\$L\n", trait.value)
        }

        val methodBuilder = createMethodSignature()
            .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)

        typeBuilder.addMethod(methodBuilder.build())
        return JavaFile.builder(packageName, typeBuilder.build()).build()
    }

    private fun generateStub(interfaceName: String, interfacePackage: String, interfaceType: ClassName): JavaFile {
        val stubName = "${interfaceName}Stub"
        val stubPackage = "$interfacePackage.stub"
        
        val typeBuilder = TypeSpec.classBuilder(stubName)
            .addModifiers(Modifier.PUBLIC)
            .addSuperinterface(interfaceType)

        val methodBuilder = createMethodSignature()
            .addModifiers(Modifier.PUBLIC)
            .addAnnotation(Override::class.java)
            .addStatement("throw new \$T(\$T.NOT_IMPLEMENTED, \$S)", 
                ClassName.get("org.springframework.web.server", "ResponseStatusException"),
                ClassName.get("org.springframework.http", "HttpStatus"),
                "Operation ${operation.id.name} has not been implemented yet.")

        typeBuilder.addMethod(methodBuilder.build())
        return JavaFile.builder(stubPackage, typeBuilder.build()).build()
    }

    private fun createMethodSignature(): MethodSpec.Builder {
        val methodName = StringUtils.uncapitalize(operation.id.name)
        val methodBuilder = MethodSpec.methodBuilder(methodName)

        operation.getTrait(DocumentationTrait::class.java).ifPresent { trait ->
            methodBuilder.addJavadoc("\$L\n", trait.value)
        }

        // Handle Input
        operation.input.ifPresent { inputId ->
            val inputShape = model.expectShape(inputId, StructureShape::class.java)
            val (metadataMembers, payloadMembers) = inputShape.getMetadataAndPayload()
            
            for (member in metadataMembers) {
                val memberSymbol = symbolProvider.toSymbol(member)
                val paramName = symbolProvider.toMemberName(member)
                methodBuilder.addParameter(memberSymbol.toTypeName(), paramName)
                member.getTrait(DocumentationTrait::class.java).ifPresent { trait ->
                    methodBuilder.addJavadoc("@param \$L \$L\n", paramName, trait.value)
                }
            }

            if (payloadMembers.size == 1) {
                val member = payloadMembers[0]
                val memberSymbol = symbolProvider.toSymbol(member)
                val paramName = symbolProvider.toMemberName(member)
                methodBuilder.addParameter(memberSymbol.toTypeName(), paramName)
                member.getTrait(DocumentationTrait::class.java).ifPresent { trait ->
                    methodBuilder.addJavadoc("@param \$L \$L\n", paramName, trait.value)
                }
            }
        }

        // Handle Output
        val outputSymbol = if (operation.output.isPresent) {
            symbolProvider.toSymbol(model.expectShape(operation.output.get()))
        } else {
            null
        }
        
        val responseEntity = ClassName.get("org.springframework.http", "ResponseEntity")
        if (outputSymbol != null) {
            val outputTypeName = outputSymbol.toTypeName()
            if (useResponseEntity) {
                methodBuilder.returns(ParameterizedTypeName.get(responseEntity, outputTypeName))
            } else {
                methodBuilder.returns(outputTypeName)
            }
        } else {
            if (useResponseEntity) {
                methodBuilder.returns(ParameterizedTypeName.get(responseEntity, ClassName.get("java.lang", "Void")))
            } else {
                methodBuilder.returns(TypeName.VOID)
            }
        }

        return methodBuilder
    }
}