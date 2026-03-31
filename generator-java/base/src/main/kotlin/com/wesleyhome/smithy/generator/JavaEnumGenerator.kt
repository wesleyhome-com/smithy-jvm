package com.wesleyhome.smithy.generator

import com.palantir.javapoet.ClassName
import com.palantir.javapoet.JavaFile
import com.palantir.javapoet.MethodSpec
import com.palantir.javapoet.TypeSpec
import software.amazon.smithy.codegen.core.SymbolProvider
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.shapes.EnumShape
import software.amazon.smithy.model.shapes.IntEnumShape
import software.amazon.smithy.model.shapes.Shape
import software.amazon.smithy.model.traits.DeprecatedTrait
import software.amazon.smithy.model.traits.DocumentationTrait
import javax.lang.model.element.Modifier

/**
 * Generates a Java enum for Smithy 2 `enum` and `intEnum` shapes.
 */
class JavaEnumGenerator(
    private val codegenContext: JavaCodegenContext? = null
) : ShapeGenerator<Shape> {
    override val shapeType: Class<Shape> = Shape::class.java

    override fun generate(shape: Shape, model: Model, symbolProvider: SymbolProvider): ShapeGenerator.Result {
        if (shape !is EnumShape && shape !is IntEnumShape) {
            return ShapeGenerator.Result()
        }

        val isIntEnum = shape is IntEnumShape
        val valueType = if (isIntEnum) Integer::class.java else String::class.java

        val symbol = symbolProvider.toSymbol(shape)
        val className = ClassName.get(symbol.namespace, symbol.name)

        val typeBuilder = TypeSpec.enumBuilder(className)
            .addModifiers(Modifier.PUBLIC)

        if (shape.hasTrait(DeprecatedTrait::class.java)) {
            typeBuilder.addAnnotation(Deprecated::class.java)
        }

        shape.getTrait(DocumentationTrait::class.java).ifPresent { trait ->
            typeBuilder.addJavadoc($$"$L\n", trait.value)
        }

        if (shape is EnumShape) {
            for (member in shape.allMembers.values) {
                val name = member.memberName
                val value =
                    member.expectTrait(software.amazon.smithy.model.traits.EnumValueTrait::class.java).stringValue.get()
                val enumConstantBuilder = TypeSpec.anonymousClassBuilder($$"$S", value)
                if (member.hasTrait(DeprecatedTrait::class.java)) {
                    enumConstantBuilder.addAnnotation(Deprecated::class.java)
                }
                member.getTrait(DocumentationTrait::class.java).ifPresent { trait ->
                    enumConstantBuilder.addJavadoc($$"$L\n", trait.value)
                }
                typeBuilder.addEnumConstant(name, enumConstantBuilder.build())
            }
        } else if (shape is IntEnumShape) {
            for (member in shape.allMembers.values) {
                val name = member.memberName
                val value =
                    member.expectTrait(software.amazon.smithy.model.traits.EnumValueTrait::class.java).intValue.get()
                val enumConstantBuilder = TypeSpec.anonymousClassBuilder($$"$L", value)
                if (member.hasTrait(DeprecatedTrait::class.java)) {
                    enumConstantBuilder.addAnnotation(Deprecated::class.java)
                }
                member.getTrait(DocumentationTrait::class.java).ifPresent { trait ->
                    enumConstantBuilder.addJavadoc($$"$L\n", trait.value)
                }
                typeBuilder.addEnumConstant(name, enumConstantBuilder.build())
            }
        }

        // Add fallback constant for backward compatibility
        val fallbackValueStr = if (isIntEnum) "null" else $$"$S"
        val fallbackValueArg = if (isIntEnum) emptyArray<Any>() else arrayOf("UNKNOWN_TO_SDK_VERSION")

        val fallbackBuilder = TypeSpec.anonymousClassBuilder(fallbackValueStr, *fallbackValueArg)
        codegenContext?.let { ctx ->
            ctx.javaPoetIntegrations.forEach { integration ->
                integration.onEnumUnknownConstantGenerated(ctx, shape, fallbackBuilder)
            }
        }
        typeBuilder.addEnumConstant("UNKNOWN_TO_SDK_VERSION", fallbackBuilder.build())

        // Add value field and constructor
        typeBuilder.addField(valueType, "value", Modifier.PRIVATE, Modifier.FINAL)

        typeBuilder.addMethod(
            MethodSpec.constructorBuilder()
                .addParameter(valueType, "value")
                .addStatement("this.value = value")
                .build()
        )

        // Add @JsonValue getter
        val getterBuilder = MethodSpec.methodBuilder("getValue")
            .addModifiers(Modifier.PUBLIC)
            .returns(valueType)
            .addStatement("return value")
        codegenContext?.let { ctx ->
            ctx.javaPoetIntegrations.forEach { integration ->
                integration.onEnumValueGetterGenerated(ctx, shape, getterBuilder)
            }
        }

        typeBuilder.addMethod(getterBuilder.build())

        // Add toString for String enums specifically if needed
        if (!isIntEnum) {
            typeBuilder.addMethod(
                MethodSpec.methodBuilder("toString")
                    .addAnnotation(Override::class.java)
                    .addModifiers(Modifier.PUBLIC)
                    .returns(String::class.java)
                    .addStatement("return String.valueOf(value)")
                    .build()
            )
        }

        // Add @JsonCreator factory method
        val creatorBuilder = MethodSpec.methodBuilder("fromValue")
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
            .returns(className)
            .addParameter(valueType, "value")
            .beginControlFlow("if (value == null)")
            .addStatement("return UNKNOWN_TO_SDK_VERSION")
            .endControlFlow()
            .beginControlFlow($$"for ($T b : $T.values())", className, className)
            .beginControlFlow("if (value.equals(b.value))")
            .addStatement("return b")
            .endControlFlow()
            .endControlFlow()
            .addStatement("return UNKNOWN_TO_SDK_VERSION")
        codegenContext?.let { ctx ->
            ctx.javaPoetIntegrations.forEach { integration ->
                integration.onEnumFromValueGenerated(ctx, shape, creatorBuilder)
            }
        }

        typeBuilder.addMethod(creatorBuilder.build())
        codegenContext?.let { ctx ->
            ctx.javaPoetIntegrations.forEach { integration ->
                integration.onShapeGenerated(ctx, shape, typeBuilder)
            }
        }

        val javaFile = JavaFile.builder(symbol.namespace, typeBuilder.build()).build()
        return ShapeGenerator.Result(listOf(javaFile.toGeneratedFile()))
    }
}
