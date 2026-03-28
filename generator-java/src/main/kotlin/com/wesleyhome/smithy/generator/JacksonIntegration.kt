package com.wesleyhome.smithy.generator

import com.palantir.javapoet.AnnotationSpec
import com.palantir.javapoet.ClassName
import com.palantir.javapoet.ParameterSpec
import software.amazon.smithy.model.shapes.MemberShape

class JacksonIntegration : JavaCodegenIntegration {
	private val jsonProperty = ClassName.get("com.fasterxml.jackson.annotation", "JsonProperty")

	override fun name(): String = "jackson-integration"

	override fun onRecordMemberGenerated(
		context: JavaCodegenContext,
		member: MemberShape,
		parameter: ParameterSpec.Builder
	) {
		if (resolveSerializationLibrary(context) != "jackson") {
			return
		}

		parameter.addAnnotation(
			AnnotationSpec.builder(jsonProperty)
				.addMember("value", $$"$S", member.memberName)
				.build()
		)
	}

	private fun resolveSerializationLibrary(context: JavaCodegenContext): String {
		val configured = context.settings.getString("serializationLibrary")
		return when (context.target) {
			JavaCodegenTarget.SERVER -> configured ?: "jackson"
			JavaCodegenTarget.MODEL -> configured ?: "jackson"
			JavaCodegenTarget.CLIENT -> configured ?: "none"
		}
	}
}
