package com.wesleyhome.smithy.generator

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.doesNotContain
import com.palantir.javapoet.ParameterSpec
import org.junit.jupiter.api.Test
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.node.Node
import software.amazon.smithy.model.shapes.MemberShape
import software.amazon.smithy.model.shapes.ServiceShape
import software.amazon.smithy.model.shapes.ShapeId
import software.amazon.smithy.model.shapes.StructureShape

class JavaStructureGeneratorIntegrationsTest {

	@Test
	fun `applies jackson and validation integrations through hooks`() {
		val model = Model.assembler()
			.addUnparsedModel(
				"test.smithy", """
                ${'$'}version: "2"
                namespace com.wesleyhome

                service TestService {
                    version: "1.0"
                    operations: [TestOp]
                }

                operation TestOp {
                    input: ValidatedStructure
                    output: ValidatedStructure
                }

                structure ValidatedStructure {
                    @required
                    name: String

                    @range(min: 10, max: 20)
                    age: Integer
                }
            """.trimIndent()
			)
			.assemble()
			.unwrap()

		val serviceShape = model.expectShape(ShapeId.from("com.wesleyhome#TestService"), ServiceShape::class.java)
		val shape = model.expectShape(ShapeId.from("com.wesleyhome#ValidatedStructure"), StructureShape::class.java)
		val symbolProvider = JavaSymbolProvider(model, "com.wesleyhome.generated", "DTO", serviceShape)

		val integrations = listOf<JavaCodegenIntegration>(ValidationIntegration(), JacksonIntegration())
		val context = JavaCodegenContext(
			model = model,
			settings = JavaSettings.from(Node.objectNodeBuilder().build()),
			serviceShape = serviceShape,
			symbolProvider = symbolProvider,
			integrations = integrations,
			target = CodegenTarget.MODEL
		)

		val code = JavaStructureGenerator(context).generate(shape, model, symbolProvider).files.first().content

		assertThat(code).contains("@NotNull @JsonProperty(\"name\") String name")
		assertThat(code).contains("@Range(min = 10, max = 20) @JsonProperty(\"age\") Integer age")
	}

	@Test
	fun `does not invoke JavaPoet hooks on JavaCodegenIntegration-only implementations`() {
		val model = Model.assembler()
			.addUnparsedModel(
				"test.smithy", """
                ${'$'}version: "2"
                namespace com.wesleyhome

                service TestService {
                    version: "1.0"
                    operations: [TestOp]
                }

                operation TestOp {
                    input: SimpleStructure
                    output: SimpleStructure
                }

                structure SimpleStructure {
                    name: String
                }
            """.trimIndent()
			)
			.assemble()
			.unwrap()

		val serviceShape = model.expectShape(ShapeId.from("com.wesleyhome#TestService"), ServiceShape::class.java)
		val shape = model.expectShape(ShapeId.from("com.wesleyhome#SimpleStructure"), StructureShape::class.java)
		val symbolProvider = JavaSymbolProvider(model, "com.wesleyhome.generated", "DTO", serviceShape)

		val integrations = listOf<JavaCodegenIntegration>(LegacyLikeHooksIntegration())
		val context = JavaCodegenContext(
			model = model,
			settings = JavaSettings.from(Node.objectNodeBuilder().build()),
			serviceShape = serviceShape,
			symbolProvider = symbolProvider,
			integrations = integrations,
			target = CodegenTarget.MODEL
		)

		val code = JavaStructureGenerator(context).generate(shape, model, symbolProvider).files.first().content

		assertThat(code).doesNotContain("@Deprecated String name")
	}

	private class LegacyLikeHooksIntegration : JavaCodegenIntegration {
		override fun name(): String = "legacy-like-hooks"

		@Suppress("unused")
		fun onRecordMemberGenerated(
			context: JavaCodegenContext,
			member: MemberShape,
			parameter: ParameterSpec.Builder
		) {
			parameter.addAnnotation(Deprecated::class.java)
		}
	}
}
