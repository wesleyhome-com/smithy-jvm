package com.wesleyhome.smithy.generator

import com.palantir.javapoet.ClassName
import software.amazon.smithy.model.shapes.MemberShape

data class JavaUnionVariant(
	val member: MemberShape,
	val variantClassName: ClassName
)
