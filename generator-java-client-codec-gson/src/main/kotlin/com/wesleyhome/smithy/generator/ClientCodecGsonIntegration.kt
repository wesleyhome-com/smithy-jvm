package com.wesleyhome.smithy.generator

class ClientCodecGsonIntegration : JavaCodegenIntegration {
	override fun name(): String = "client-codec-gson-integration"

	override fun supports(target: JavaCodegenTarget): Boolean = target == JavaCodegenTarget.CLIENT

	override fun generatorContributions(context: JavaCodegenContext): List<GeneratorContribution> = listOf(
		GeneratorContribution(
			family = GeneratorFamilies.CLIENT_PROTOCOL_CODEC_GSON,
			generators = listOf(JavaClientGsonCodecGenerator())
		)
	)
}
