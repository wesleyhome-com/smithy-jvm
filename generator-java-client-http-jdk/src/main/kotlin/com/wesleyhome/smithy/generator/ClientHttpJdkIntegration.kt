package com.wesleyhome.smithy.generator

class ClientHttpJdkIntegration : JavaCodegenIntegration {
	override fun name(): String = "client-http-jdk-integration"

	override fun supports(target: JavaCodegenTarget): Boolean = target == JavaCodegenTarget.CLIENT

	override fun generatorContributions(context: JavaCodegenContext): List<JavaGeneratorContribution> = listOf(
		JavaGeneratorContribution(
			family = JavaGeneratorFamilies.CLIENT_HTTP_TRANSPORT_JDK,
			generators = listOf(JavaClientJdkHttpTransportGenerator())
		)
	)
}
