package com.wesleyhome.smithy.generator

import com.palantir.javapoet.AnnotationSpec
import com.palantir.javapoet.ClassName
import com.palantir.javapoet.MethodSpec
import com.palantir.javapoet.ParameterSpec
import com.palantir.javapoet.TypeSpec
import software.amazon.smithy.model.shapes.MemberShape
import software.amazon.smithy.model.shapes.Shape
import software.amazon.smithy.model.shapes.StructureShape
import software.amazon.smithy.model.shapes.UnionShape

class JacksonIntegration : JavaCodegenIntegration, JavaPoetCodegenIntegration {
	private val jsonProperty = ClassName.get("com.fasterxml.jackson.annotation", "JsonProperty")

	override fun name(): String = "jackson-integration"

	override fun onRecordMemberGenerated(
		context: JavaCodegenContext,
		member: MemberShape,
		parameter: ParameterSpec.Builder
	) {
		parameter.addAnnotation(
			AnnotationSpec.builder(jsonProperty)
				.addMember("value", $$"$S", member.memberName)
				.build()
		)
	}

	override fun onExceptionDtoParameterGenerated(
		context: JavaCodegenContext,
		errorShape: StructureShape,
		wireName: String,
		parameter: ParameterSpec.Builder
	) {
		parameter.addAnnotation(
			AnnotationSpec.builder(jsonProperty)
				.addMember("value", $$"$S", wireName)
				.build()
		)
	}

	override fun onEnumUnknownConstantGenerated(
		context: JavaCodegenContext,
		shape: Shape,
		constantBuilder: TypeSpec.Builder
	) {
		constantBuilder.addAnnotation(ClassName.get("com.fasterxml.jackson.annotation", "JsonEnumDefaultValue"))
	}

	override fun onEnumValueGetterGenerated(
		context: JavaCodegenContext,
		shape: Shape,
		getterBuilder: MethodSpec.Builder
	) {
		getterBuilder.addAnnotation(ClassName.get("com.fasterxml.jackson.annotation", "JsonValue"))
	}

	override fun onEnumFromValueGenerated(
		context: JavaCodegenContext,
		shape: Shape,
		creatorBuilder: MethodSpec.Builder
	) {
		creatorBuilder.addAnnotation(ClassName.get("com.fasterxml.jackson.annotation", "JsonCreator"))
	}

	override fun onUnionGenerated(
		context: JavaCodegenContext,
		shape: UnionShape,
		typeBuilder: TypeSpec.Builder,
		unknownClassName: ClassName,
		variants: List<JavaUnionVariant>
	) {
		typeBuilder.addAnnotation(
			AnnotationSpec.builder(ClassName.get("com.fasterxml.jackson.annotation", "JsonTypeInfo"))
				.addMember("use", $$"$T.Id.NAME", ClassName.get("com.fasterxml.jackson.annotation", "JsonTypeInfo"))
				.addMember(
					"include",
					$$"$T.As.WRAPPER_OBJECT",
					ClassName.get("com.fasterxml.jackson.annotation", "JsonTypeInfo")
				)
				.addMember("defaultImpl", $$"$T.class", unknownClassName)
				.build()
		)

		val subTypesBuilder = AnnotationSpec.builder(ClassName.get("com.fasterxml.jackson.annotation", "JsonSubTypes"))
		for (variant in variants) {
			subTypesBuilder.addMember(
				"value",
				$$"$L",
				AnnotationSpec.builder(ClassName.get("com.fasterxml.jackson.annotation", "JsonSubTypes", "Type"))
					.addMember("value", $$"$T.class", variant.variantClassName)
					.addMember("name", $$"$S", variant.member.memberName)
					.build()
			)
		}
		typeBuilder.addAnnotation(subTypesBuilder.build())
	}
}
