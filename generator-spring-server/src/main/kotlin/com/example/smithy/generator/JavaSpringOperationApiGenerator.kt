package com.example.smithy.generator

import com.palantir.javapoet.*
import software.amazon.smithy.codegen.core.SymbolProvider
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.shapes.OperationShape
import software.amazon.smithy.model.shapes.ServiceShape
import software.amazon.smithy.model.shapes.StructureShape
import software.amazon.smithy.model.knowledge.TopDownIndex
import software.amazon.smithy.utils.StringUtils
import javax.lang.model.element.Modifier

/**
 * Generates Spring Boot API interfaces and default stubs for a Smithy ServiceShape.
 */
class JavaSpringOperationApiGenerator : ShapeGenerator<ServiceShape> {
    override val shapeType: Class<ServiceShape> = ServiceShape::class.java

    private val jakartaValid = ClassName.get("jakarta.validation", "Valid")
    private val httpStatus = ClassName.get("org.springframework.http", "HttpStatus")
    private val responseStatusException = ClassName.get("org.springframework.web.server", "ResponseStatusException")

    override fun generate(shape: ServiceShape, model: Model, symbolProvider: SymbolProvider): ShapeGenerator.Result {
        val generatedFiles = mutableListOf<GeneratedFile>()
        
        val topDownIndex = TopDownIndex.of(model)
        val operations = topDownIndex.getContainedOperations(shape)
        
        for (operation in operations) {
            val interfaceFile = generateApiInterface(operation, model, symbolProvider)
            generatedFiles.add(interfaceFile.toGeneratedFile())
            
            val stubFile = generateApiStub(operation, model, symbolProvider)
            generatedFiles.add(stubFile.toGeneratedFile())
        }

        return ShapeGenerator.Result(generatedFiles)
    }

    private fun generateApiInterface(operation: OperationShape, model: Model, symbolProvider: SymbolProvider): JavaFile {
        val symbol = symbolProvider.toSymbol(operation)
        val packageName = symbol.namespace
        val interfaceName = symbol.name
        
        val typeBuilder = TypeSpec.interfaceBuilder(interfaceName)
            .addModifiers(Modifier.PUBLIC)
            
        operation.getTrait(software.amazon.smithy.model.traits.DocumentationTrait::class.java).ifPresent { trait ->
            typeBuilder.addJavadoc("\$L\n", trait.value)
        }
        
        val methodBuilder = buildMethodSignature(operation, model, symbolProvider)
        methodBuilder.addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
        typeBuilder.addMethod(methodBuilder.build())

        return JavaFile.builder(packageName, typeBuilder.build()).build()
    }

    private fun generateApiStub(operation: OperationShape, model: Model, symbolProvider: SymbolProvider): JavaFile {
        val symbol = symbolProvider.toSymbol(operation)
        val apiPackageName = symbol.namespace
        val interfaceName = symbol.name
        val stubName = "${interfaceName}Stub"
        val stubPackageName = "$apiPackageName.stub"
        
        val typeBuilder = TypeSpec.classBuilder(stubName)
            .addModifiers(Modifier.PUBLIC)
            .addSuperinterface(ClassName.get(apiPackageName, interfaceName))
            .addAnnotation(ClassName.get("org.springframework.stereotype", "Component"))
            .addAnnotation(AnnotationSpec.builder(ClassName.get("org.springframework.boot.autoconfigure.condition", "ConditionalOnMissingBean"))
                .addMember("value", "\$T.class", ClassName.get(apiPackageName, interfaceName))
                .build())
            .addJavadoc("Default implementation of {@link \$L} that returns 501 Not Implemented.\n", interfaceName)

        val methodBuilder = buildMethodSignature(operation, model, symbolProvider)
            .addAnnotation(Override::class.java)
            .addModifiers(Modifier.PUBLIC)
            .addStatement("throw new \$T(\$T.NOT_IMPLEMENTED, \"Operation \$L not implemented\")", 
                responseStatusException, httpStatus, operation.id.name)
        
        typeBuilder.addMethod(methodBuilder.build())

        return JavaFile.builder(stubPackageName, typeBuilder.build()).build()
    }

    private fun buildMethodSignature(operation: OperationShape, model: Model, symbolProvider: SymbolProvider): MethodSpec.Builder {
        val methodName = StringUtils.uncapitalize(operation.id.name)
        val methodBuilder = MethodSpec.methodBuilder(methodName)
        
        operation.getTrait(software.amazon.smithy.model.traits.DocumentationTrait::class.java).ifPresent { trait ->
            methodBuilder.addJavadoc("\$L\n\n", trait.value)
        }

        // Map Input (Parameters)
        operation.input.ifPresent { inputId ->
            val inputShape = model.expectShape(inputId, StructureShape::class.java)
            val (metadataMembers, payloadMembers) = inputShape.getMetadataAndPayload()

            for (member in metadataMembers) {
                val memberSymbol = symbolProvider.toSymbol(member)
                val paramName = symbolProvider.toMemberName(member)
                methodBuilder.addParameter(ParameterSpec.builder(memberSymbol.toTypeName(), paramName)
                    .addAnnotation(jakartaValid)
                    .build())
                
                member.getTrait(software.amazon.smithy.model.traits.DocumentationTrait::class.java).ifPresent { trait ->
                    methodBuilder.addJavadoc("@param \$L \$L\n", paramName, trait.value)
                }
            }

            if (payloadMembers.size == 1) {
                val member = payloadMembers[0]
                val memberSymbol = symbolProvider.toSymbol(member)
                val paramName = symbolProvider.toMemberName(member)
                methodBuilder.addParameter(ParameterSpec.builder(memberSymbol.toTypeName(), paramName)
                    .addAnnotation(jakartaValid)
                    .build())
                    
                member.getTrait(software.amazon.smithy.model.traits.DocumentationTrait::class.java).ifPresent { trait ->
                    methodBuilder.addJavadoc("@param \$L \$L\n", paramName, trait.value)
                }
            }
        }

        // Map Output (Return Type)
        if (operation.output.isPresent) {
            val outputSymbol = symbolProvider.toSymbol(model.expectShape(operation.output.get()))
            methodBuilder.returns(outputSymbol.toTypeName())
            methodBuilder.addJavadoc("@return \$T\n", outputSymbol.toTypeName())
        } else {
            methodBuilder.returns(TypeName.VOID)
        }

        return methodBuilder
    }
}
