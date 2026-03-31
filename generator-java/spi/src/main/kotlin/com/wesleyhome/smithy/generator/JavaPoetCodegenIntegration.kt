package com.wesleyhome.smithy.generator

import com.palantir.javapoet.ClassName
import com.palantir.javapoet.MethodSpec
import com.palantir.javapoet.ParameterSpec
import com.palantir.javapoet.TypeSpec
import software.amazon.smithy.model.shapes.MemberShape
import software.amazon.smithy.model.shapes.Shape
import software.amazon.smithy.model.shapes.StructureShape
import software.amazon.smithy.model.shapes.UnionShape

/**
 * JavaPoet-specific hooks for mutating generated Java declarations.
 *
 * Keep implementations focused on Java emission concerns (annotations, modifiers, method decoration).
 * Lifecycle/model/generator selection concerns belong in [JavaCodegenIntegration].
 */
interface JavaPoetCodegenIntegration : JavaCodegenIntegration {
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
