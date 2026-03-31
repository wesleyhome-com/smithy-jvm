package com.wesleyhome.smithy.generator

import com.palantir.javapoet.AnnotationSpec
import com.palantir.javapoet.ClassName
import com.palantir.javapoet.CodeBlock
import com.palantir.javapoet.FieldSpec
import com.palantir.javapoet.JavaFile
import com.palantir.javapoet.MethodSpec
import com.palantir.javapoet.ParameterSpec
import com.palantir.javapoet.ParameterizedTypeName
import com.palantir.javapoet.TypeSpec
import software.amazon.smithy.codegen.core.SymbolProvider
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.knowledge.TopDownIndex
import software.amazon.smithy.model.shapes.OperationShape
import software.amazon.smithy.model.shapes.ServiceShape
import software.amazon.smithy.model.shapes.Shape
import software.amazon.smithy.model.shapes.StructureShape
import software.amazon.smithy.model.traits.DefaultTrait
import software.amazon.smithy.model.traits.HttpHeaderTrait
import software.amazon.smithy.model.traits.HttpLabelTrait
import software.amazon.smithy.model.traits.HttpQueryTrait
import software.amazon.smithy.model.traits.HttpTrait
import software.amazon.smithy.model.traits.RequiredTrait
import software.amazon.smithy.model.traits.TagsTrait
import software.amazon.smithy.model.validation.Severity
import software.amazon.smithy.model.validation.ValidationEvent
import software.amazon.smithy.utils.StringUtils
import javax.lang.model.element.Modifier

/**
 * Generates Spring Boot RestControllers for a Smithy ServiceShape, grouped by tags.
 */
class JavaSpringControllerGenerator : ShapeGenerator<ServiceShape> {
	override val shapeType: Class<ServiceShape> = ServiceShape::class.java

	private val springWeb = "org.springframework.web.bind.annotation"
	private val jakartaValid = ClassName.get("jakarta.validation", "Valid")
	private val restController = ClassName.get(springWeb, "RestController")
	private val pathVariable = ClassName.get(springWeb, "PathVariable")
	private val requestParam = ClassName.get(springWeb, "RequestParam")
	private val requestHeader = ClassName.get(springWeb, "RequestHeader")
	private val requestBody = ClassName.get(springWeb, "RequestBody")
	private val responseEntity = ClassName.get("org.springframework.http", "ResponseEntity")

	override fun generate(shape: ServiceShape, model: Model, symbolProvider: SymbolProvider): ShapeGenerator.Result {
		val validationEvents = mutableListOf<ValidationEvent>()
		val generatedFiles = mutableListOf<GeneratedFile>()

		val serviceSymbol = symbolProvider.toSymbol(shape)
		val topDownIndex = TopDownIndex.of(model)
		val operations = topDownIndex.getContainedOperations(shape).toList()

		// Group Operations by Tag for Controllers
		val groupedOperations = operations.groupBy { getPrimaryTag(it) }

		for ((tag, ops) in groupedOperations) {
			val controllerName = if (tag != null) {
				"${StringUtils.capitalize(tag)}Controller"
			} else {
				"${serviceSymbol.name}Controller"
			}
			// We assume controllers go into a .controller subpackage relative to the service
			val controllerPackage = "${serviceSymbol.namespace}.controller"

			val (javaFile, events) = generateController(controllerName, controllerPackage, ops, model, symbolProvider)
			generatedFiles.add(javaFile.toGeneratedFile())
			validationEvents.addAll(events)
		}

		return ShapeGenerator.Result(generatedFiles, validationEvents)
	}

	private fun generateController(
		controllerName: String,
		packageName: String,
		operations: List<OperationShape>,
		model: Model,
		symbolProvider: SymbolProvider
	): Pair<JavaFile, List<ValidationEvent>> {
		val validationEvents = mutableListOf<ValidationEvent>()
		val typeBuilder = TypeSpec.classBuilder(controllerName)
			.addModifiers(Modifier.PUBLIC)
			.addAnnotation(restController)

		val constructorBuilder = MethodSpec.constructorBuilder()
			.addModifiers(Modifier.PUBLIC)

		for (operation in operations) {
			val operationSymbol = symbolProvider.toSymbol(operation)
			val apiInterfaceName = operationSymbol.name
			val apiInterfacePackage = operationSymbol.namespace
			val apiInterfaceType = ClassName.get(apiInterfacePackage, apiInterfaceName)
			val fieldName = StringUtils.uncapitalize(apiInterfaceName)

			// Add Field
			typeBuilder.addField(
				FieldSpec.builder(apiInterfaceType, fieldName, Modifier.PRIVATE, Modifier.FINAL)
					.build()
			)
			// Add Constructor Parameter
			constructorBuilder.addParameter(apiInterfaceType, fieldName)
			constructorBuilder.addStatement($$"this.$L = $L", fieldName, fieldName)

			// Add Controller Method
			val operationMethodName = StringUtils.uncapitalize(operation.id.name)
			val methodBuilder = MethodSpec.methodBuilder(operationMethodName)
				.addModifiers(Modifier.PUBLIC)

			// Handle HTTP Trait
			operation.getTrait(HttpTrait::class.java).ifPresent { httpTrait ->
				val annotationName = when (httpTrait.method.uppercase()) {
					"GET" -> "GetMapping"
					"POST" -> "PostMapping"
					"PUT" -> "PutMapping"
					"DELETE" -> "DeleteMapping"
					"PATCH" -> "PatchMapping"
					else -> "RequestMapping"
				}

				val httpAnnotation = AnnotationSpec.builder(ClassName.get(springWeb, annotationName))
					.addMember("value", $$"$S", httpTrait.uri.toString())

				if (annotationName == "RequestMapping") {
					httpAnnotation.addMember("method", $$"RequestMethod.$L", httpTrait.method.uppercase())
				}

				methodBuilder.addAnnotation(httpAnnotation.build())
			}

			// Handle Input/Output
			var skipOperation = false
			operation.input.ifPresent { inputId ->
				val inputShape = model.expectShape(inputId, StructureShape::class.java)
				val (metadataMembers, payloadMembers) = inputShape.getMetadataAndPayload()

				if (payloadMembers.size > 1) {
					val event = ValidationEvent.builder()
						.id("MultiplePayloadMembers")
						.severity(Severity.ERROR)
						.shape(operation)
						.message("Spring Boot requires a single @RequestBody. Please wrap these members in a dedicated structure and apply the @httpPayload trait to it in your Smithy model.")
						.build()
					validationEvents.add(event)
					skipOperation = true
					return@ifPresent
				}

				for (member in metadataMembers) {
					val memberSymbol = symbolProvider.toSymbol(member)
					val paramName = symbolProvider.toMemberName(member)
					val typeName = memberSymbol.toTypeName()
					val paramBuilder = ParameterSpec.builder(typeName, paramName).addAnnotation(jakartaValid)

					if (member.hasTrait(HttpLabelTrait::class.java)) {
						paramBuilder.addAnnotation(
							AnnotationSpec.builder(pathVariable)
								.addMember("value", $$"$S", paramName)
								.build()
						)
					} else if (member.hasTrait(HttpQueryTrait::class.java)) {
						val queryTrait = member.expectTrait(HttpQueryTrait::class.java)
						val annotationBuilder = AnnotationSpec.builder(requestParam)
							.addMember("value", $$"$S", queryTrait.value)
							.addMember("required", $$"$L", member.hasTrait(RequiredTrait::class.java))

						member.getTrait(DefaultTrait::class.java).ifPresent { defaultTrait ->
							val node = defaultTrait.toNode()
							val valueStr = if (node.isStringNode) {
								node.expectStringNode().value
							} else {
								node.toString()
							}
							annotationBuilder.addMember("defaultValue", $$"$S", valueStr)
						}

						paramBuilder.addAnnotation(annotationBuilder.build())
					} else if (member.hasTrait(HttpHeaderTrait::class.java)) {
						val headerTrait = member.expectTrait(HttpHeaderTrait::class.java)
						paramBuilder.addAnnotation(
							AnnotationSpec.builder(requestHeader)
								.addMember("value", $$"$S", headerTrait.value)
								.addMember("required", $$"$L", member.hasTrait(RequiredTrait::class.java))
								.build()
						)
					}
					methodBuilder.addParameter(paramBuilder.build())
				}

				if (payloadMembers.size == 1) {
					val member = payloadMembers[0]
					val memberSymbol = symbolProvider.toSymbol(member)
					val paramName = symbolProvider.toMemberName(member)
					val typeName = memberSymbol.toTypeName()
					methodBuilder.addParameter(
						ParameterSpec.builder(typeName, paramName)
							.addAnnotation(jakartaValid)
							.addAnnotation(requestBody)
							.build()
					)
				}
			}

			if (skipOperation) continue

			// Build call to API
			val callArgs = operation.input.map { inputId ->
				val inputShape = model.expectShape(inputId, StructureShape::class.java)
				val (metadataMembers, payloadMembers) = inputShape.getMetadataAndPayload()

				(metadataMembers + payloadMembers)
					.joinToString(", ") { symbolProvider.toMemberName(it) }
			}.orElse("")

			val outputSymbol = if (operation.output.isPresent) {
				symbolProvider.toSymbol(model.expectShape(operation.output.get()))
			} else null

			if (outputSymbol != null) {
				val outputTypeName = outputSymbol.toTypeName()
				val outputShape = model.expectShape(operation.output.get(), StructureShape::class.java)

				methodBuilder.returns(ParameterizedTypeName.get(responseEntity, outputTypeName))
				methodBuilder.addStatement(
					$$"$T result = $L.$L($L)",
					outputTypeName,
					fieldName,
					operationMethodName,
					callArgs
				)

				val builderChain = CodeBlock.builder().add($$"$T.ok()", responseEntity)

				val (metadataMembers, _) = outputShape.getMetadataAndPayload()
				for (member in metadataMembers.filter { it.hasTrait(HttpHeaderTrait::class.java) }) {
					val headerName = member.expectTrait(HttpHeaderTrait::class.java).value
					val getterName = symbolProvider.toMemberName(member)
					// DTO is a record, so we use getterName()
					builderChain.add(
						$$"\n.header($S, $T.valueOf(result.$L()))",
						headerName,
						String::class.java,
						getterName
					)
				}

				builderChain.add("\n.body(result)")
				methodBuilder.addStatement($$"return $L", builderChain.build())

			} else {
				methodBuilder.returns(ParameterizedTypeName.get(responseEntity, ClassName.get("java.lang", "Void")))
				methodBuilder.addStatement($$"$L.$L($L)", fieldName, operationMethodName, callArgs)
				methodBuilder.addStatement($$"return $T.ok().build()", responseEntity)
			}

			typeBuilder.addMethod(methodBuilder.build())
		}

		typeBuilder.addMethod(constructorBuilder.build())

		return Pair(JavaFile.builder(packageName, typeBuilder.build()).build(), validationEvents)
	}

	private fun getPrimaryTag(shape: Shape): String? {
		return shape.getTrait(TagsTrait::class.java).map { it.values.firstOrNull() }.orElse(null)
	}
}
