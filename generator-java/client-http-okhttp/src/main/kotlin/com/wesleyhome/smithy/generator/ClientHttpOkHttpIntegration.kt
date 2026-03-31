package com.wesleyhome.smithy.generator

class ClientHttpOkHttpIntegration : JavaCodegenIntegration {
	override fun name(): String = "client-http-okhttp-integration"

	override fun supports(target: CodegenTarget): Boolean = target == CodegenTarget.CLIENT

	override fun generatorContributions(context: JavaCodegenContext): List<GeneratorContribution> = listOf(
		GeneratorContribution(
			family = GeneratorFamilies.CLIENT_HTTP_TRANSPORT_OKHTTP,
			generators = listOf(JavaClientOkHttpTransportGenerator())
		)
	)
}
