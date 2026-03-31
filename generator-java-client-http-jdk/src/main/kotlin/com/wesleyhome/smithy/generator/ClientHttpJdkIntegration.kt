package com.wesleyhome.smithy.generator

class ClientHttpJdkIntegration : JavaCodegenIntegration {
	override fun name(): String = "client-http-jdk-integration"

	override fun supports(target: JavaCodegenTarget): Boolean = target == JavaCodegenTarget.CLIENT

	override fun generatorContributions(context: JavaCodegenContext): List<GeneratorContribution> = listOf(
		GeneratorContribution(
			family = GeneratorFamilies.CLIENT_HTTP_TRANSPORT_JDK,
			generators = listOf(JavaClientJdkHttpTransportGenerator())
		)
	)
}
