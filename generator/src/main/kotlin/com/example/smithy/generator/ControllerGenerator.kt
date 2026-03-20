package com.example.smithy.generator

import com.palantir.javapoet.*
import software.amazon.smithy.codegen.core.SymbolProvider
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.shapes.OperationShape
import software.amazon.smithy.model.shapes.StructureShape
import software.amazon.smithy.model.traits.*
import software.amazon.smithy.model.validation.Severity
import software.amazon.smithy.model.validation.ValidationEvent
import software.amazon.smithy.utils.StringUtils
import javax.lang.model.element.Modifier

class ControllerGenerator(
    private val model: Model,
    private val symbolProvider: SymbolProvider,
    private val controllerName: String,
    private val packageName: String,
    private val operations: List<OperationShape>,
    private val useResponseEntity: Boolean = true
) {
    private val springWeb = "org.springframework.web.bind.annotation"
    private val jakartaValid = ClassName.get("jakarta.validation", "Valid")
    private val restController = ClassName.get(springWeb, "RestController")
    private val pathVariable = ClassName.get(springWeb, "PathVariable")
    private val requestParam = ClassName.get(springWeb, "RequestParam")
    private val requestHeader = ClassName.get(springWeb, "RequestHeader")
    private val requestBody = ClassName.get(springWeb, "RequestBody")

    fun generate(): Pair<JavaFile, List<ValidationEvent>> {
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
            typeBuilder.addField(FieldSpec.builder(apiInterfaceType, fieldName, Modifier.PRIVATE, Modifier.FINAL).build())

            // Add Constructor Parameter
            constructorBuilder.addParameter(apiInterfaceType, fieldName)
            constructorBuilder.addStatement("this.\$L = \$L", fieldName, fieldName)

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
                    .addMember("value", "\$S", httpTrait.uri.toString())
                
                if (annotationName == "RequestMapping") {
                    httpAnnotation.addMember("method", "RequestMethod.\$L", httpTrait.method.uppercase())
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
                        paramBuilder.addAnnotation(AnnotationSpec.builder(pathVariable)
                            .addMember("value", "\$S", paramName)
                            .build())
                    } else if (member.hasTrait(HttpQueryTrait::class.java)) {
                        val queryTrait = member.expectTrait(HttpQueryTrait::class.java)
                        val annotationBuilder = AnnotationSpec.builder(requestParam)
                            .addMember("value", "\$S", queryTrait.value)
                            .addMember("required", "\$L", member.hasTrait(RequiredTrait::class.java))
                        
                        member.getTrait(DefaultTrait::class.java).ifPresent { defaultTrait ->
                            val node = defaultTrait.toNode()
                            val valueStr = if (node.isStringNode) {
                                node.expectStringNode().value
                            } else {
                                node.toString()
                            }
                            annotationBuilder.addMember("defaultValue", "\$S", valueStr)
                        }
                        
                        paramBuilder.addAnnotation(annotationBuilder.build())
                    } else if (member.hasTrait(HttpHeaderTrait::class.java)) {
                        val headerTrait = member.expectTrait(HttpHeaderTrait::class.java)
                        paramBuilder.addAnnotation(AnnotationSpec.builder(requestHeader)
                            .addMember("value", "\$S", headerTrait.value)
                            .addMember("required", "\$L", member.hasTrait(RequiredTrait::class.java))
                            .build())
                    }
                    methodBuilder.addParameter(paramBuilder.build())
                }

                if (payloadMembers.size == 1) {
                    val member = payloadMembers[0]
                    val memberSymbol = symbolProvider.toSymbol(member)
                    val paramName = symbolProvider.toMemberName(member)
                    val typeName = memberSymbol.toTypeName()
                    methodBuilder.addParameter(ParameterSpec.builder(typeName, paramName)
                        .addAnnotation(jakartaValid)
                        .addAnnotation(requestBody)
                        .build())
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

            val responseEntity = ClassName.get("org.springframework.http", "ResponseEntity")
            val outputSymbol = if (operation.output.isPresent) {
                symbolProvider.toSymbol(model.expectShape(operation.output.get()))
            } else {
                null
            }
            
            if (outputSymbol != null) {
                val outputTypeName = outputSymbol.toTypeName()
                if (useResponseEntity) {
                    methodBuilder.returns(ParameterizedTypeName.get(responseEntity, outputTypeName))
                } else {
                    methodBuilder.returns(outputTypeName)
                }
                methodBuilder.addStatement("return \$L.\$L(\$L)", fieldName, operationMethodName, callArgs)
            } else {
                if (useResponseEntity) {
                    methodBuilder.returns(ParameterizedTypeName.get(responseEntity, ClassName.get("java.lang", "Void")))
                    methodBuilder.addStatement("return \$L.\$L(\$L)", fieldName, operationMethodName, callArgs)
                } else {
                    methodBuilder.returns(TypeName.VOID)
                    methodBuilder.addStatement("\$L.\$L(\$L)", fieldName, operationMethodName, callArgs)
                }
            }

            typeBuilder.addMethod(methodBuilder.build())
        }

        typeBuilder.addMethod(constructorBuilder.build())

        return Pair(JavaFile.builder(packageName, typeBuilder.build()).build(), validationEvents)
    }
}
