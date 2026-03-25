package com.wesleyhome.smithy.generator

import com.palantir.javapoet.*
import software.amazon.smithy.codegen.core.SymbolProvider
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.shapes.UnionShape
import software.amazon.smithy.utils.StringUtils
import javax.lang.model.element.Modifier

/**
 * Generates a Java sealed interface for a Smithy UnionShape.
 */
class JavaUnionGenerator(
    private val serializationLibrary: String = "jackson"
) : ShapeGenerator<UnionShape> {
    override val shapeType: Class<UnionShape> = UnionShape::class.java

    override fun generate(shape: UnionShape, model: Model, symbolProvider: SymbolProvider): ShapeGenerator.Result {
        val symbol = symbolProvider.toSymbol(shape)
        val className = ClassName.get(symbol.namespace, symbol.name)
        
        val unknownName = "Unknown"
        val unknownClassName = className.nestedClass(unknownName)

        val typeBuilder = TypeSpec.interfaceBuilder(className)
            .addModifiers(Modifier.PUBLIC, Modifier.SEALED)

        if (serializationLibrary == "jackson") {
            typeBuilder.addAnnotation(AnnotationSpec.builder(ClassName.get("com.fasterxml.jackson.annotation", "JsonTypeInfo"))
                .addMember("use", "\$T.Id.NAME", ClassName.get("com.fasterxml.jackson.annotation", "JsonTypeInfo"))
                .addMember("include", "\$T.As.WRAPPER_OBJECT", ClassName.get("com.fasterxml.jackson.annotation", "JsonTypeInfo"))
                .addMember("defaultImpl", "\$T.class", unknownClassName)
                .build())
        }

        if (shape.hasTrait(software.amazon.smithy.model.traits.DeprecatedTrait::class.java)) {
            typeBuilder.addAnnotation(Deprecated::class.java)
        }

        val subTypesBuilder = AnnotationSpec.builder(ClassName.get("com.fasterxml.jackson.annotation", "JsonSubTypes"))

        for (member in shape.allMembers.values) {
            val memberName = StringUtils.capitalize(member.memberName)
            val variantClassName = className.nestedClass(memberName)
            val memberSymbol = symbolProvider.toSymbol(member)
            val typeName = memberSymbol.toTypeName()

            if (serializationLibrary == "jackson") {
                subTypesBuilder.addMember("value", "\$L", 
                    AnnotationSpec.builder(ClassName.get("com.fasterxml.jackson.annotation", "JsonSubTypes", "Type"))
                        .addMember("value", "\$T.class", variantClassName)
                        .addMember("name", "\$S", member.memberName)
                        .build())
            }

            val recordBuilder = TypeSpec.recordBuilder(memberName)
                .addModifiers(Modifier.FINAL, Modifier.PUBLIC, Modifier.STATIC)
                .addSuperinterface(className)
            
            if (member.hasTrait(software.amazon.smithy.model.traits.DeprecatedTrait::class.java)) {
                recordBuilder.addAnnotation(Deprecated::class.java)
            }

            recordBuilder.recordConstructor(MethodSpec.constructorBuilder()
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
        
        if (serializationLibrary == "jackson") {
            typeBuilder.addAnnotation(subTypesBuilder.build())
        }

        val javaFile = JavaFile.builder(symbol.namespace, typeBuilder.build()).build()
        return ShapeGenerator.Result(listOf(javaFile.toGeneratedFile()))
    }
}
