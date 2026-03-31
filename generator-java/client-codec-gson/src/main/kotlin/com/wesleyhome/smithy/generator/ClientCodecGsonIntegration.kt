package com.wesleyhome.smithy.generator

class ClientCodecGsonIntegration : JavaCodegenIntegration {
	override fun name(): String = "client-codec-gson-integration"

	override fun supports(target: CodegenTarget): Boolean = target == CodegenTarget.CLIENT

	override fun generatorContributions(context: JavaCodegenContext): List<GeneratorContribution> = listOf(
		GeneratorContribution(
			family = GeneratorFamilies.CLIENT_PROTOCOL_CODEC_GSON,
			generators = listOf(JavaClientGsonCodecGenerator())
		)
	)
}
