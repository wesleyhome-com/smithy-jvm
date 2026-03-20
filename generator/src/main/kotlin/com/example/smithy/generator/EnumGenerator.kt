package com.example.smithy.generator

import com.palantir.javapoet.*
import software.amazon.smithy.codegen.core.SymbolProvider
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.shapes.EnumShape
import software.amazon.smithy.model.shapes.Shape
import software.amazon.smithy.model.shapes.StringShape
import software.amazon.smithy.model.traits.DocumentationTrait
import software.amazon.smithy.model.traits.EnumTrait
import javax.lang.model.element.Modifier

class EnumGenerator(
    private val model: Model,
    private val symbolProvider: SymbolProvider,
    private val shape: Shape
) {
    fun generate(): JavaFile {
        val symbol = symbolProvider.toSymbol(shape)
        val className = ClassName.get(symbol.namespace, symbol.name)
        
        val typeBuilder = TypeSpec.enumBuilder(className)
            .addModifiers(Modifier.PUBLIC)

        shape.getTrait(DocumentationTrait::class.java).ifPresent { trait ->
            typeBuilder.addJavadoc("\$L\n", trait.value)
        }

        if (shape is EnumShape) {
            for (member in shape.allMembers.values) {
                val name = member.memberName
                val value = member.expectTrait(software.amazon.smithy.model.traits.EnumValueTrait::class.java).stringValue.get()
                val enumConstantBuilder = TypeSpec.anonymousClassBuilder("\$S", value)
                member.getTrait(DocumentationTrait::class.java).ifPresent { trait ->
                    enumConstantBuilder.addJavadoc("\$L\n", trait.value)
                }
                typeBuilder.addEnumConstant(name, enumConstantBuilder.build())
            }
        } else if (shape is StringShape && shape.hasTrait(EnumTrait::class.java)) {
            val enumTrait = shape.expectTrait(EnumTrait::class.java)
            for (definition in enumTrait.values) {
                val name = definition.name.orElseGet { definition.value.uppercase().replace(" ", "_") }
                val value = definition.value
                val enumConstantBuilder = TypeSpec.anonymousClassBuilder("\$S", value)
                definition.documentation.ifPresent { doc ->
                    enumConstantBuilder.addJavadoc("\$L\n", doc)
                }
                typeBuilder.addEnumConstant(name, enumConstantBuilder.build())
            }
        }

        // Add fallback constant for backward compatibility
        typeBuilder.addEnumConstant("UNKNOWN_TO_SDK_VERSION", TypeSpec.anonymousClassBuilder("\$S", "UNKNOWN_TO_SDK_VERSION")
            .addAnnotation(ClassName.get("com.fasterxml.jackson.annotation", "JsonEnumDefaultValue"))
            .build())

        // Add value field and constructor
        typeBuilder.addField(String::class.java, "value", Modifier.PRIVATE, Modifier.FINAL)
        
        typeBuilder.addMethod(MethodSpec.constructorBuilder()
            .addParameter(String::class.java, "value")
            .addStatement("this.value = value")
            .build())

        // Add @JsonValue getter
        typeBuilder.addMethod(MethodSpec.methodBuilder("toString")
            .addAnnotation(Override::class.java)
            .addAnnotation(ClassName.get("com.fasterxml.jackson.annotation", "JsonValue"))
            .addModifiers(Modifier.PUBLIC)
            .returns(String::class.java)
            .addStatement("return value")
            .build())

        // Add @JsonCreator factory method
        val creatorBuilder = MethodSpec.methodBuilder("fromValue")
            .addAnnotation(ClassName.get("com.fasterxml.jackson.annotation", "JsonCreator"))
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
            .returns(className)
            .addParameter(String::class.java, "value")
            .beginControlFlow("for (\$T b : \$T.values())", className, className)
            .beginControlFlow("if (b.value.equals(value))")
            .addStatement("return b")
            .endControlFlow()
            .endControlFlow()
            .addStatement("return UNKNOWN_TO_SDK_VERSION")

        typeBuilder.addMethod(creatorBuilder.build())

        return JavaFile.builder(symbol.namespace, typeBuilder.build()).build()
    }
}
