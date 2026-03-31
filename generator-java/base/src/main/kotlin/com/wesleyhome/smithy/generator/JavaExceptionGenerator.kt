package com.wesleyhome.smithy.generator

import com.palantir.javapoet.ClassName
import com.palantir.javapoet.FieldSpec
import com.palantir.javapoet.JavaFile
import com.palantir.javapoet.MethodSpec
import com.palantir.javapoet.ParameterSpec
import com.palantir.javapoet.TypeSpec
import software.amazon.smithy.codegen.core.SymbolProvider
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.shapes.StructureShape
import software.amazon.smithy.model.traits.ErrorTrait
import javax.lang.model.element.Modifier

/**
 * Generates a Java exception for a Smithy StructureShape with the @error trait.
 */
class JavaExceptionGenerator(
    private val codegenContext: JavaCodegenContext? = null
) : ShapeGenerator<StructureShape> {
    override val shapeType: Class<StructureShape> = StructureShape::class.java

    override fun generate(shape: StructureShape, model: Model, symbolProvider: SymbolProvider): ShapeGenerator.Result {
        // We only generate exceptions for structures that HAVE the @error trait
        if (!shape.hasTrait(ErrorTrait::class.java)) {
            return ShapeGenerator.Result()
        }

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

            typeBuilder.addMethod(
                MethodSpec.methodBuilder("get${fieldName.replaceFirstChar { it.uppercase() }}")
                    .addModifiers(Modifier.PUBLIC)
                    .returns(typeName)
                    .addStatement($$"return this.$L", fieldName)
                    .build()
            )
        }

        // Add Standard Constructor
        val standardConstructor = MethodSpec.constructorBuilder()
            .addModifiers(Modifier.PUBLIC)
            .addParameter(String::class.java, "message")

        for (member in shape.allMembers.values) {
            if (member.memberName.equals("message", ignoreCase = true)) continue

            val memberSymbol = symbolProvider.toSymbol(member)
            val fieldName = symbolProvider.toMemberName(member)
            val typeName = memberSymbol.toTypeName()
            standardConstructor.addParameter(typeName, fieldName)
        }

        standardConstructor.addStatement("super(message)")
        for (member in shape.allMembers.values) {
            if (member.memberName.equals("message", ignoreCase = true)) continue

            val fieldName = symbolProvider.toMemberName(member)
            standardConstructor.addStatement($$"this.$L = $L", fieldName, fieldName)
        }

        typeBuilder.addMethod(standardConstructor.build())

        // Add DTO support for clean serialization
        generateDtoSupport(typeBuilder, className, shape, symbolProvider)

        // Add constructor that takes the DTO for easy deserialization mapping
        val dtoConstructor = MethodSpec.constructorBuilder()
            .addModifiers(Modifier.PUBLIC)
            .addParameter(className.nestedClass("Data"), "data")
            .addStatement(
                $$"this(data.message()$L)", shape.allMembers.values
                    .filter { !it.memberName.equals("message", ignoreCase = true) }
                    .joinToString("") { ", data.${symbolProvider.toMemberName(it)}()" })

        typeBuilder.addMethod(dtoConstructor.build())
        codegenContext?.let { ctx ->
            ctx.javaPoetIntegrations.forEach { integration ->
                integration.onShapeGenerated(ctx, shape, typeBuilder)
            }
        }

        val javaFile = JavaFile.builder(symbol.namespace, typeBuilder.build()).build()
        return ShapeGenerator.Result(listOf(javaFile.toGeneratedFile()))
    }

    private fun generateDtoSupport(
        typeBuilder: TypeSpec.Builder,
        className: ClassName,
        shape: StructureShape,
        symbolProvider: SymbolProvider
    ) {
        val dtoRecordName = "Data"

        val nonMessageMembers = shape.allMembers.values.filter { !it.memberName.equals("message", ignoreCase = true) }

        val recordParameters = mutableListOf<ParameterSpec>()

        val messageParam = ParameterSpec.builder(String::class.java, "message")
        codegenContext?.let { ctx ->
            ctx.javaPoetIntegrations.forEach { integration ->
                integration.onExceptionDtoParameterGenerated(ctx, shape, "message", messageParam)
            }
        }
        recordParameters.add(messageParam.build())

        nonMessageMembers.forEach { member ->
            val memberSymbol = symbolProvider.toSymbol(member)
            val fieldName = symbolProvider.toMemberName(member)
            val typeName = memberSymbol.toTypeName()

            val paramBuilder = ParameterSpec.builder(typeName, fieldName)
            codegenContext?.let { ctx ->
                ctx.javaPoetIntegrations.forEach { integration ->
                    integration.onExceptionDtoParameterGenerated(ctx, shape, member.memberName, paramBuilder)
                }
            }
            recordParameters.add(paramBuilder.build())
        }

        val toDtoArgs = listOf("getMessage()") + nonMessageMembers.map { member ->
            symbolProvider.toMemberName(member)
        }

        val dtoRecord = TypeSpec.recordBuilder(dtoRecordName)
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
            .recordConstructor(
                MethodSpec.constructorBuilder()
                    .addParameters(recordParameters)
                    .build()
            )
            .build()

        val toDtoMethod = MethodSpec.methodBuilder("toDto")
            .addModifiers(Modifier.PUBLIC)
            .returns(className.nestedClass(dtoRecordName))
            .addStatement($$"return new $L($L)", dtoRecordName, toDtoArgs.joinToString(", "))
            .build()

        typeBuilder.addType(dtoRecord)
        typeBuilder.addMethod(toDtoMethod)
    }
}
