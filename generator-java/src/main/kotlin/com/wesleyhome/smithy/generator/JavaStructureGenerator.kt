package com.wesleyhome.smithy.generator

import com.palantir.javapoet.AnnotationSpec
import com.palantir.javapoet.ClassName
import com.palantir.javapoet.JavaFile
import com.palantir.javapoet.MethodSpec
import com.palantir.javapoet.ParameterSpec
import com.palantir.javapoet.TypeSpec
import software.amazon.smithy.codegen.core.SymbolProvider
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.shapes.ListShape
import software.amazon.smithy.model.shapes.MapShape
import software.amazon.smithy.model.shapes.MemberShape
import software.amazon.smithy.model.shapes.StructureShape
import software.amazon.smithy.model.shapes.UnionShape
import software.amazon.smithy.model.traits.DefaultTrait
import software.amazon.smithy.model.traits.DeprecatedTrait
import software.amazon.smithy.model.traits.DocumentationTrait
import software.amazon.smithy.model.traits.ErrorTrait
import software.amazon.smithy.model.traits.LengthTrait
import software.amazon.smithy.model.traits.PatternTrait
import software.amazon.smithy.model.traits.RangeTrait
import software.amazon.smithy.model.traits.RequiredTrait
import javax.lang.model.element.Modifier

/**
 * Generates a Java record for a Smithy StructureShape.
 */
class JavaStructureGenerator(
    private val serializationLibrary: String = "jackson"
) : ShapeGenerator<StructureShape> {
    override val shapeType: Class<StructureShape> = StructureShape::class.java

    private val jsonProperty = ClassName.get("com.fasterxml.jackson.annotation", "JsonProperty")
    private val jakartaValid = ClassName.get("jakarta.validation", "Valid")
    private val constraints = "jakarta.validation.constraints"

    override fun generate(shape: StructureShape, model: Model, symbolProvider: SymbolProvider): ShapeGenerator.Result {
        // We only generate structures that are NOT errors (ErrorTrait is handled by JavaExceptionGenerator)
        if (shape.hasTrait(ErrorTrait::class.java)) {
            return ShapeGenerator.Result()
        }

        val symbol = symbolProvider.toSymbol(shape)
        val className = ClassName.get(symbol.namespace, symbol.name)

        val recordParameters = shape.allMembers.values.map { member ->
            val memberSymbol = symbolProvider.toSymbol(member)
            val fieldName = symbolProvider.toMemberName(member)
            val typeName = memberSymbol.toTypeName()
            val paramBuilder = ParameterSpec.builder(
                typeName,
                fieldName
            )
            applyValidation(paramBuilder, member, model)
            applyDocumentation(paramBuilder, member)
            if (member.hasTrait(DeprecatedTrait::class.java)) {
                paramBuilder.addAnnotation(Deprecated::class.java)
            }

            if (serializationLibrary == "jackson") {
                paramBuilder.addAnnotation(
                    AnnotationSpec.builder(jsonProperty)
                        .addMember("value", $$"$S", member.memberName)
                        .build()
                )
            }

            paramBuilder.build()
        }
        val typeBuilder = TypeSpec.recordBuilder(className)
            .addModifiers(Modifier.PUBLIC)

        if (shape.hasTrait(DeprecatedTrait::class.java)) {
            typeBuilder.addAnnotation(Deprecated::class.java)
        }

        applyDocumentation(typeBuilder, shape)

        // Define the record components via the recordConstructor
        typeBuilder.recordConstructor(
            MethodSpec.constructorBuilder()
                .addParameters(recordParameters)
                .build()
        )

        // Add a compact constructor for handling @default values if any are present
        val hasDefaults = shape.allMembers.values.any { it.hasTrait(DefaultTrait::class.java) }

        if (hasDefaults) {
            val compactConstructor = MethodSpec.compactConstructorBuilder()
                .addModifiers(Modifier.PUBLIC)

            for (member in shape.allMembers.values) {
                member.getTrait(DefaultTrait::class.java).ifPresent { defaultTrait ->
                    val fieldName = symbolProvider.toMemberName(member)
                    val node = defaultTrait.toNode()
                    val valueStr = if (node.isStringNode) {
                        "\"${node.expectStringNode().value}\""
                    } else if (node.isBooleanNode) {
                        node.expectBooleanNode().value.toString()
                    } else {
                        node.toString()
                    }
                    compactConstructor.addStatement($$"if ($L == null) $L = $L", fieldName, fieldName, valueStr)
                }
            }
            typeBuilder.addMethod(compactConstructor.build())
        }

        val javaFile = JavaFile.builder(symbol.namespace, typeBuilder.build()).build()
        return ShapeGenerator.Result(listOf(javaFile.toGeneratedFile()))
    }

    private fun applyDocumentation(builder: TypeSpec.Builder, shape: software.amazon.smithy.model.shapes.Shape) {
        shape.getTrait(DocumentationTrait::class.java).ifPresent { trait ->
            builder.addJavadoc($$"$L\n", trait.value)
        }
    }

    private fun applyDocumentation(builder: ParameterSpec.Builder, member: MemberShape) {
        member.getTrait(DocumentationTrait::class.java).ifPresent { trait ->
            builder.addJavadoc($$"$L\n", trait.value)
        }
    }

    private fun applyValidation(fieldBuilder: ParameterSpec.Builder, member: MemberShape, model: Model) {
        if (member.hasTrait(RequiredTrait::class.java)) {
            fieldBuilder.addAnnotation(ClassName.get(constraints, "NotNull"))
        }

        member.getTrait(LengthTrait::class.java).ifPresent { trait ->
            val annotation = AnnotationSpec.builder(ClassName.get(constraints, "Size"))
            trait.min.ifPresent { annotation.addMember("min", $$"$L", it) }
            trait.max.ifPresent { annotation.addMember("max", $$"$L", it) }
            fieldBuilder.addAnnotation(annotation.build())
        }

        member.getTrait(RangeTrait::class.java).ifPresent { trait ->
            if (trait.min.isPresent && trait.max.isPresent) {
                fieldBuilder.addAnnotation(
                    AnnotationSpec.builder(ClassName.get("org.hibernate.validator.constraints", "Range"))
                        .addMember("min", $$"$L", trait.min.get())
                        .addMember("max", $$"$L", trait.max.get()).build()
                )
            } else {
                trait.min.ifPresent {
                    fieldBuilder.addAnnotation(
                        AnnotationSpec.builder(ClassName.get(constraints, "Min"))
                            .addMember("value", $$"$L", it).build()
                    )
                }
                trait.max.ifPresent {
                    fieldBuilder.addAnnotation(
                        AnnotationSpec.builder(ClassName.get(constraints, "Max"))
                            .addMember("value", $$"$L", it).build()
                    )
                }
            }
        }

        member.getTrait(PatternTrait::class.java).ifPresent { trait ->
            fieldBuilder.addAnnotation(
                AnnotationSpec.builder(ClassName.get(constraints, "Pattern"))
                    .addMember("regexp", $$"$S", trait.value).build()
            )
        }

        // Add @Valid for nested structures or collections of structures
        val target = model.expectShape(member.target)
        if (target is StructureShape || target is ListShape || target is MapShape || target is UnionShape) {
            fieldBuilder.addAnnotation(jakartaValid)
        }
    }
}
