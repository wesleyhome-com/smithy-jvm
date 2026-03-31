package com.wesleyhome.smithy.generator

import com.palantir.javapoet.AnnotationSpec
import com.palantir.javapoet.ClassName
import com.palantir.javapoet.ParameterSpec
import software.amazon.smithy.model.shapes.ListShape
import software.amazon.smithy.model.shapes.MapShape
import software.amazon.smithy.model.shapes.MemberShape
import software.amazon.smithy.model.shapes.StructureShape
import software.amazon.smithy.model.shapes.UnionShape
import software.amazon.smithy.model.traits.LengthTrait
import software.amazon.smithy.model.traits.PatternTrait
import software.amazon.smithy.model.traits.RangeTrait
import software.amazon.smithy.model.traits.RequiredTrait

class ValidationIntegration : JavaPoetCodegenIntegration {
	private val jakartaValid = ClassName.get("jakarta.validation", "Valid")
	private val constraints = "jakarta.validation.constraints"

	override fun name(): String = "validation-integration"

	override fun onRecordMemberGenerated(
		context: JavaCodegenContext,
		member: MemberShape,
		parameter: ParameterSpec.Builder
	) {
		if (member.hasTrait(RequiredTrait::class.java)) {
			parameter.addAnnotation(ClassName.get(constraints, "NotNull"))
		}

		member.getTrait(LengthTrait::class.java).ifPresent { trait ->
			val annotation = AnnotationSpec.builder(ClassName.get(constraints, "Size"))
			trait.min.ifPresent { annotation.addMember("min", $$"$L", it) }
			trait.max.ifPresent { annotation.addMember("max", $$"$L", it) }
			parameter.addAnnotation(annotation.build())
		}

		member.getTrait(RangeTrait::class.java).ifPresent { trait ->
			if (trait.min.isPresent && trait.max.isPresent) {
				parameter.addAnnotation(
					AnnotationSpec.builder(ClassName.get("org.hibernate.validator.constraints", "Range"))
						.addMember("min", $$"$L", trait.min.get())
						.addMember("max", $$"$L", trait.max.get())
						.build()
				)
			} else {
				trait.min.ifPresent {
					parameter.addAnnotation(
						AnnotationSpec.builder(ClassName.get(constraints, "Min"))
							.addMember("value", $$"$L", it)
							.build()
					)
				}
				trait.max.ifPresent {
					parameter.addAnnotation(
						AnnotationSpec.builder(ClassName.get(constraints, "Max"))
							.addMember("value", $$"$L", it)
							.build()
					)
				}
			}
		}

		member.getTrait(PatternTrait::class.java).ifPresent { trait ->
			parameter.addAnnotation(
				AnnotationSpec.builder(ClassName.get(constraints, "Pattern"))
					.addMember("regexp", $$"$S", trait.value)
					.build()
			)
		}

		val target = context.model.expectShape(member.target)
		if (target is StructureShape || target is ListShape || target is MapShape || target is UnionShape) {
			parameter.addAnnotation(jakartaValid)
		}
	}
}
