package com.wesleyhome.smithy.generator

class ClientCodecJacksonIntegration : JavaCodegenIntegration {
	override fun name(): String = "client-codec-jackson-integration"

	override fun supports(target: CodegenTarget): Boolean = target == CodegenTarget.CLIENT

	override fun generatorContributions(context: JavaCodegenContext): List<GeneratorContribution> = listOf(
		GeneratorContribution(
			family = GeneratorFamilies.CLIENT_PROTOCOL_CODEC_JACKSON,
			generators = listOf(JavaClientJacksonCodecGenerator())
		)
	)
}
