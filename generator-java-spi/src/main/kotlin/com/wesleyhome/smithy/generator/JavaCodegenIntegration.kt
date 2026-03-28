package com.wesleyhome.smithy.generator

import com.palantir.javapoet.ClassName
import com.palantir.javapoet.MethodSpec
import com.palantir.javapoet.ParameterSpec
import com.palantir.javapoet.TypeSpec
import software.amazon.smithy.codegen.core.SymbolProvider
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.shapes.MemberShape
import software.amazon.smithy.model.shapes.Shape
import software.amazon.smithy.model.shapes.StructureShape
import software.amazon.smithy.model.shapes.UnionShape

/**
 * Extension point for Java codegen customizations.
 *
 * Integrations are discovered via ServiceLoader, filtered by target, and ordered by [priority].
 * Generator-producing integrations should contribute logical families through [generatorContributions].
 *
 * Hook methods receive mutable JavaPoet builders. Implementations are expected to mutate the provided
 * builder instance in place.
 */
interface JavaCodegenIntegration {
    fun name(): String

    fun priority(): Byte = 0

    fun supports(target: JavaCodegenTarget): Boolean = true

    fun preprocessModel(context: JavaCodegenContext): Model = context.model

    fun decorateSymbolProvider(
        context: JavaCodegenContext,
        symbolProvider: SymbolProvider
    ): SymbolProvider = symbolProvider

    /**
     * Contributes generators grouped by logical family. Runner picks one winner per family by priority.
     */
    fun generatorContributions(context: JavaCodegenContext): List<JavaGeneratorContribution> {
        val generators = additionalShapeGenerators(context)
        if (generators.isEmpty()) {
            return emptyList()
        }
        return listOf(JavaGeneratorContribution(family = "${name()}:default", generators = generators))
    }

    /**
     * Legacy convenience hook. Prefer [generatorContributions].
     */
    fun additionalShapeGenerators(context: JavaCodegenContext): List<ShapeGenerator<out Shape>> = emptyList()

    fun onShapeGenerated(context: JavaCodegenContext, shape: Shape, typeSpec: TypeSpec.Builder) {}

    fun onRecordMemberGenerated(
        context: JavaCodegenContext,
        member: MemberShape,
        parameter: ParameterSpec.Builder
    ) {
    }

    fun onExceptionDtoParameterGenerated(
        context: JavaCodegenContext,
        errorShape: StructureShape,
        wireName: String,
        parameter: ParameterSpec.Builder
    ) {
    }

    fun onEnumUnknownConstantGenerated(
        context: JavaCodegenContext,
        shape: Shape,
        constantBuilder: TypeSpec.Builder
    ) {
    }

    fun onEnumValueGetterGenerated(
        context: JavaCodegenContext,
        shape: Shape,
        getterBuilder: MethodSpec.Builder
    ) {
    }

    fun onEnumFromValueGenerated(
        context: JavaCodegenContext,
        shape: Shape,
        creatorBuilder: MethodSpec.Builder
    ) {
    }

    fun onUnionGenerated(
        context: JavaCodegenContext,
        shape: UnionShape,
        typeBuilder: TypeSpec.Builder,
        unknownClassName: ClassName,
        variants: List<JavaUnionVariant>
    ) {
    }
}
