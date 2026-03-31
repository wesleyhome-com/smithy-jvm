package com.wesleyhome.smithy.generator

class ClientHttpJdkIntegration : JavaCodegenIntegration {
	override fun name(): String = "client-http-jdk-integration"

	override fun supports(target: CodegenTarget): Boolean = target == CodegenTarget.CLIENT

	override fun generatorContributions(context: JavaCodegenContext): List<GeneratorContribution> = listOf(
		GeneratorContribution(
			family = GeneratorFamilies.CLIENT_HTTP_TRANSPORT_JDK,
			generators = listOf(JavaClientJdkHttpTransportGenerator())
		)
	)
}
