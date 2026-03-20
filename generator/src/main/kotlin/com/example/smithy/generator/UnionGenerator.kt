package com.example.smithy.generator

import com.palantir.javapoet.*
import software.amazon.smithy.codegen.core.SymbolProvider
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.shapes.UnionShape
import software.amazon.smithy.utils.StringUtils
import javax.lang.model.element.Modifier

class UnionGenerator(
    private val model: Model,
    private val symbolProvider: SymbolProvider,
    private val shape: UnionShape
) {
    fun generate(): JavaFile {
        val symbol = symbolProvider.toSymbol(shape)
        val className = ClassName.get(symbol.namespace, symbol.name)
        
        val unknownName = "Unknown"
        val unknownClassName = className.nestedClass(unknownName)

        val typeBuilder = TypeSpec.interfaceBuilder(className)
            .addModifiers(Modifier.PUBLIC, Modifier.SEALED)
            .addAnnotation(AnnotationSpec.builder(ClassName.get("com.fasterxml.jackson.annotation", "JsonTypeInfo"))
                .addMember("use", "\$T.Id.NAME", ClassName.get("com.fasterxml.jackson.annotation", "JsonTypeInfo"))
                .addMember("include", "\$T.As.WRAPPER_OBJECT", ClassName.get("com.fasterxml.jackson.annotation", "JsonTypeInfo"))
                .addMember("defaultImpl", "\$T.class", unknownClassName)
                .build())

        val subTypesBuilder = AnnotationSpec.builder(ClassName.get("com.fasterxml.jackson.annotation", "JsonSubTypes"))

        for (member in shape.allMembers.values) {
            val memberName = StringUtils.capitalize(member.memberName)
            val variantClassName = className.nestedClass(memberName)
            val memberSymbol = symbolProvider.toSymbol(member)
            val typeName = memberSymbol.toTypeName()

            subTypesBuilder.addMember("value", "\$L", 
                AnnotationSpec.builder(ClassName.get("com.fasterxml.jackson.annotation", "JsonSubTypes", "Type"))
                    .addMember("value", "\$T.class", variantClassName)
                    .addMember("name", "\$S", member.memberName)
                    .build())

            val recordBuilder = TypeSpec.recordBuilder(memberName)
                .addModifiers(Modifier.FINAL, Modifier.PUBLIC, Modifier.STATIC)
                .addSuperinterface(className)
                .recordConstructor(MethodSpec.constructorBuilder()
                    .addParameter(typeName, "value")
                    .build())
            
            typeBuilder.addType(recordBuilder.build())
            typeBuilder.addPermittedSubclass(variantClassName)
        }

        // Add Unknown variant
        val unknownBuilder = TypeSpec.classBuilder(unknownName)
            .addModifiers(Modifier.PUBLIC, Modifier.FINAL, Modifier.STATIC)
            .addSuperinterface(className)

        typeBuilder.addType(unknownBuilder.build())
        typeBuilder.addPermittedSubclass(unknownClassName)
        
        typeBuilder.addAnnotation(subTypesBuilder.build())

        val javaFile = JavaFile.builder(symbol.namespace, typeBuilder.build()).build()
        return javaFile
    }
}
