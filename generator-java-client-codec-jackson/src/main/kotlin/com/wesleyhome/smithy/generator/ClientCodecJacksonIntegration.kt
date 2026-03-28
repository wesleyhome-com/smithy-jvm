package com.wesleyhome.smithy.generator

class ClientCodecJacksonIntegration : JavaCodegenIntegration {
	override fun name(): String = "client-codec-jackson-integration"

	override fun supports(target: JavaCodegenTarget): Boolean = target == JavaCodegenTarget.CLIENT

	override fun generatorContributions(context: JavaCodegenContext): List<JavaGeneratorContribution> = listOf(
		JavaGeneratorContribution(
			family = JavaGeneratorFamilies.CLIENT_PROTOCOL_CODEC_JACKSON,
			generators = listOf(JavaClientJacksonCodecGenerator())
		)
	)
}
