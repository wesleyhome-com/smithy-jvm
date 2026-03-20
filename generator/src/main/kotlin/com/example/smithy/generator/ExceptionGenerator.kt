package com.example.smithy.generator

import com.palantir.javapoet.*
import software.amazon.smithy.codegen.core.SymbolProvider
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.shapes.StructureShape
import software.amazon.smithy.model.traits.ErrorTrait
import software.amazon.smithy.model.traits.HttpErrorTrait
import javax.lang.model.element.Modifier

class ExceptionGenerator(
    private val model: Model,
    private val symbolProvider: SymbolProvider,
    private val shape: StructureShape
) {
    fun generate(): JavaFile {
        val symbol = symbolProvider.toSymbol(shape)
        val className = ClassName.get(symbol.namespace, symbol.name)
        
        val typeBuilder = TypeSpec.classBuilder(className)
            .addModifiers(Modifier.PUBLIC)
            .superclass(RuntimeException::class.java)

        // Add fields for members (errors in Smithy are structures)
        for (member in shape.allMembers.values) {
            if (member.memberName.equals("message", ignoreCase = true)) continue
            
            val memberSymbol = symbolProvider.toSymbol(member)
            val fieldName = symbolProvider.toMemberName(member)
            val typeName = memberSymbol.toTypeName()
            
            typeBuilder.addField(FieldSpec.builder(typeName, fieldName, Modifier.PRIVATE, Modifier.FINAL).build())
            
            typeBuilder.addMethod(MethodSpec.methodBuilder("get${fieldName.replaceFirstChar { it.uppercase() }}")
                .addModifiers(Modifier.PUBLIC)
                .returns(typeName)
                .addStatement("return this.\$L", fieldName)
                .build())
        }

        // Add Constructor
        val constructorBuilder = MethodSpec.constructorBuilder()
            .addModifiers(Modifier.PUBLIC)
            .addParameter(String::class.java, "message")

        for (member in shape.allMembers.values) {
            if (member.memberName.equals("message", ignoreCase = true)) continue
            
            val memberSymbol = symbolProvider.toSymbol(member)
            val fieldName = symbolProvider.toMemberName(member)
            val typeName = memberSymbol.toTypeName()
            constructorBuilder.addParameter(typeName, fieldName)
        }

        constructorBuilder.addStatement("super(message)")
        for (member in shape.allMembers.values) {
            if (member.memberName.equals("message", ignoreCase = true)) continue
            
            val fieldName = symbolProvider.toMemberName(member)
            constructorBuilder.addStatement("this.\$L = \$L", fieldName, fieldName)
        }
        
        typeBuilder.addMethod(constructorBuilder.build())

        // Add DTO support for clean serialization
        generateDtoSupport(typeBuilder, className)

        return JavaFile.builder(symbol.namespace, typeBuilder.build()).build()
    }

    private fun generateDtoSupport(typeBuilder: TypeSpec.Builder, className: ClassName) {
        val dtoRecordName = "Data"
        val jsonProperty = ClassName.get("com.fasterxml.jackson.annotation", "JsonProperty")

        val nonMessageMembers = shape.allMembers.values.filter { !it.memberName.equals("message", ignoreCase = true) }

        val recordParameters = listOf(
            ParameterSpec.builder(String::class.java, "message")
                .addAnnotation(AnnotationSpec.builder(jsonProperty).addMember("value", "\$S", "message").build())
                .build()
        ) + nonMessageMembers.map { member ->
            val memberSymbol = symbolProvider.toSymbol(member)
            val fieldName = symbolProvider.toMemberName(member)
            val typeName = memberSymbol.toTypeName()
            
            ParameterSpec.builder(typeName, fieldName)
                .addAnnotation(AnnotationSpec.builder(jsonProperty).addMember("value", "\$S", member.memberName).build())
                .build()
        }

        val toDtoArgs = listOf("getMessage()") + nonMessageMembers.map { member ->
            symbolProvider.toMemberName(member)
        }

        val dtoRecord = TypeSpec.recordBuilder(dtoRecordName)
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
            .recordConstructor(MethodSpec.constructorBuilder()
                .addParameters(recordParameters)
                .build())
            .build()

        val toDtoMethod = MethodSpec.methodBuilder("toDto")
            .addModifiers(Modifier.PUBLIC)
            .returns(className.nestedClass(dtoRecordName))
            .addStatement("return new \$L(\$L)", dtoRecordName, toDtoArgs.joinToString(", "))
            .build()

        typeBuilder.addType(dtoRecord)
        typeBuilder.addMethod(toDtoMethod)
    }
}
