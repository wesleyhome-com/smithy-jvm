package com.wesleyhome.smithy.generator

import software.amazon.smithy.codegen.core.SymbolProvider
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.shapes.ServiceShape

data class JavaCodegenContext(
	val model: Model,
	val settings: CodegenSettings,
	val serviceShape: ServiceShape,
	val symbolProvider: SymbolProvider,
	val integrations: List<JavaCodegenIntegration>,
	val target: CodegenTarget
) {
	val javaPoetIntegrations: List<JavaPoetCodegenIntegration>
		get() = integrations.filterIsInstance<JavaPoetCodegenIntegration>()
}
