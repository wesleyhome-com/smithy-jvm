package com.wesleyhome.smithy.generator

import com.palantir.javapoet.ClassName
import com.palantir.javapoet.JavaFile
import com.palantir.javapoet.MethodSpec
import com.palantir.javapoet.TypeName
import com.palantir.javapoet.TypeSpec
import software.amazon.smithy.codegen.core.SymbolProvider
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.knowledge.TopDownIndex
import software.amazon.smithy.model.shapes.OperationShape
import software.amazon.smithy.model.shapes.ServiceShape
import software.amazon.smithy.model.shapes.StructureShape
import software.amazon.smithy.model.traits.DocumentationTrait
import software.amazon.smithy.model.traits.HttpHeaderTrait
import software.amazon.smithy.model.traits.HttpLabelTrait
import software.amazon.smithy.model.traits.HttpQueryTrait
import software.amazon.smithy.model.traits.HttpTrait
import software.amazon.smithy.utils.StringUtils
import javax.lang.model.element.Modifier

/**
 * Generates a production-ready Java Client for a Smithy Service, using the Builder pattern.
 */
class JavaClientGenerator : ShapeGenerator<ServiceShape> {
    override val shapeType: Class<ServiceShape> = ServiceShape::class.java

    override fun generate(shape: ServiceShape, model: Model, symbolProvider: SymbolProvider): ShapeGenerator.Result {
        val serviceSymbol = symbolProvider.toSymbol(shape)
        val interfaceName = "${serviceSymbol.name}Client"
        val implementationName = "Default${serviceSymbol.name}Client"
        val packageName = "${serviceSymbol.namespace}.client"

        val interfaceBuilder = TypeSpec.interfaceBuilder(interfaceName)
            .addModifiers(Modifier.PUBLIC)
            .addJavadoc("Lightweight client for ${shape.id.name}.\n\n")
            .addJavadoc("This client is thread-safe and relies on a pluggable transport and serialization layer.\n")
            .addJavadoc("Use the {@link #builder()} to instantiate a new client.\n")

        val implementationBuilder = TypeSpec.classBuilder(implementationName)
            .addModifiers(Modifier.FINAL) // package-private
            .addSuperinterface(ClassName.get(packageName, interfaceName))
            .addJavadoc($$"Internal implementation of {@link $L}.\n", interfaceName)

        // 1. Implementation Fields & Constructor
        implementationBuilder.addField(
            ClassName.get(packageName, "HttpTransport"),
            "transport",
            Modifier.PRIVATE,
            Modifier.FINAL
        )
        implementationBuilder.addField(
            ClassName.get(packageName, "ProtocolCodec"),
            "codec",
            Modifier.PRIVATE,
            Modifier.FINAL
        )
        implementationBuilder.addField(String::class.java, "baseUrl", Modifier.PRIVATE, Modifier.FINAL)

        implementationBuilder.addMethod(
            MethodSpec.constructorBuilder()
                .addJavadoc("Creates a new client implementation.\n\n")
                .addJavadoc("@param transport The transport layer.\n")
                .addJavadoc("@param codec The serialization layer.\n")
                .addJavadoc("@param baseUrl The base URL.\n")
                .addParameter(ClassName.get(packageName, "HttpTransport"), "transport")
                .addParameter(ClassName.get(packageName, "ProtocolCodec"), "codec")
                .addParameter(String::class.java, "baseUrl")
                .addStatement("this.transport = transport")
                .addStatement("this.codec = codec")
                .addStatement("this.baseUrl = baseUrl.endsWith(\"/\") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl")
                .build()
        )

        // 2. Generate Operations
        val topDownIndex = TopDownIndex.of(model)
        val operations = topDownIndex.getContainedOperations(shape)

        for (operation in operations) {
            val (interfaceMethod, implMethod) = generateOperationMethod(operation, model, symbolProvider, packageName)
            interfaceBuilder.addMethod(interfaceMethod)
            implementationBuilder.addMethod(implMethod)
        }

        // 3. Generate Builder
        val builderInterface = generateBuilderInterface(packageName, interfaceName)
        val builderImpl = generateBuilderImplementation(packageName, interfaceName, implementationName)

        interfaceBuilder.addType(builderInterface)
        interfaceBuilder.addMethod(
            MethodSpec.methodBuilder("builder")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addJavadoc("Creates a new builder for configuring the client.\n\n")
                .addJavadoc("@return A new builder instance.\n")
                .returns(ClassName.get(packageName, interfaceName, "Builder"))
                .addStatement($$"return new $T()", ClassName.get(packageName, "Default${interfaceName}Builder"))
                .build()
        )

        val files = listOf(
            JavaFile.builder(packageName, interfaceBuilder.build()).build(),
            JavaFile.builder(packageName, implementationBuilder.build()).build(),
            JavaFile.builder(packageName, builderImpl).build()
        )

        return ShapeGenerator.Result(files.map { it.toGeneratedFile() })
    }

    private fun generateBuilderInterface(packageName: String, interfaceName: String): TypeSpec {
        val builderInterfaceType = ClassName.get(packageName, interfaceName, "Builder")

        val builder = TypeSpec.interfaceBuilder("Builder")
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
            .addJavadoc($$"Builder for configuring and creating a {@link $L}.\n", interfaceName)
            .addMethod(
                MethodSpec.methodBuilder("baseUrl")
                    .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                    .addJavadoc("Sets the base URL for the service.\n\n")
                    .addJavadoc("@param baseUrl The base URL.\n")
                    .addJavadoc("@return This builder.\n")
                    .addParameter(String::class.java, "baseUrl")
                    .returns(builderInterfaceType)
                    .build()
            )
            .addMethod(
                MethodSpec.methodBuilder("transport")
                    .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                    .addJavadoc("Sets the underlying HTTP transport implementation.\n\n")
                    .addJavadoc("@param transport The transport implementation.\n")
                    .addJavadoc("@return This builder.\n")
                    .addParameter(ClassName.get(packageName, "HttpTransport"), "transport")
                    .returns(builderInterfaceType)
                    .build()
            )
            .addMethod(
                MethodSpec.methodBuilder("codec")
                    .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                    .addJavadoc("Sets the serialization codec implementation.\n\n")
                    .addJavadoc("@param codec The codec implementation.\n")
                    .addJavadoc("@return This builder.\n")
                    .addParameter(ClassName.get(packageName, "ProtocolCodec"), "codec")
                    .returns(builderInterfaceType)
                    .build()
            )

        builder.addMethod(
            MethodSpec.methodBuilder("build")
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .addJavadoc("Builds and returns the configured client.\n\n")
                .addJavadoc("@return The client instance.\n")
                .returns(ClassName.get(packageName, interfaceName))
                .build()
        )

        return builder.build()
    }

    private fun generateBuilderImplementation(
        packageName: String,
        interfaceName: String,
        implementationName: String
    ): TypeSpec {
        val builderImplName = "Default${interfaceName}Builder"
        val builderInterfaceType = ClassName.get(packageName, interfaceName, "Builder")

        val builder = TypeSpec.classBuilder(builderImplName)
            .addModifiers(Modifier.FINAL) // package-private
            .addSuperinterface(builderInterfaceType)
            .addJavadoc("Internal builder implementation.\n")
            .addField(String::class.java, "baseUrl", Modifier.PRIVATE)
            .addField(ClassName.get(packageName, "HttpTransport"), "transport", Modifier.PRIVATE)
            .addField(ClassName.get(packageName, "ProtocolCodec"), "codec", Modifier.PRIVATE)

        builder.addMethod(
            MethodSpec.methodBuilder("baseUrl")
                .addAnnotation(Override::class.java)
                .addModifiers(Modifier.PUBLIC)
                .addParameter(String::class.java, "baseUrl")
                .returns(builderInterfaceType)
                .addStatement("this.baseUrl = baseUrl")
                .addStatement("return this")
                .build()
        )

        builder.addMethod(
            MethodSpec.methodBuilder("transport")
                .addAnnotation(Override::class.java)
                .addModifiers(Modifier.PUBLIC)
                .addParameter(ClassName.get(packageName, "HttpTransport"), "transport")
                .returns(builderInterfaceType)
                .addStatement("this.transport = transport")
                .addStatement("return this")
                .build()
        )

        builder.addMethod(
            MethodSpec.methodBuilder("codec")
                .addAnnotation(Override::class.java)
                .addModifiers(Modifier.PUBLIC)
                .addParameter(ClassName.get(packageName, "ProtocolCodec"), "codec")
                .returns(builderInterfaceType)
                .addStatement("this.codec = codec")
                .addStatement("return this")
                .build()
        )

        val buildMethod = MethodSpec.methodBuilder("build")
            .addAnnotation(Override::class.java)
            .addModifiers(Modifier.PUBLIC)
            .returns(ClassName.get(packageName, interfaceName))
            .beginControlFlow("if (this.baseUrl == null)")
            .addStatement($$"throw new $T(\"Base URL must be configured\")", IllegalStateException::class.java)
            .endControlFlow()

        buildMethod.beginControlFlow("if (this.transport == null)")
            .addStatement(
                $$"throw new $T(\"HttpTransport must be configured\")",
                IllegalStateException::class.java
            )
            .endControlFlow()

        buildMethod.beginControlFlow("if (this.codec == null)")
            .addStatement(
                $$"throw new $T(\"ProtocolCodec must be configured\")",
                IllegalStateException::class.java
            )
            .endControlFlow()

        buildMethod.addStatement(
            $$"return new $T(this.transport, this.codec, this.baseUrl)",
            ClassName.get(packageName, implementationName)
        )

        builder.addMethod(buildMethod.build())
        return builder.build()
    }

    private fun generateOperationMethod(
        operation: OperationShape,
        model: Model,
        symbolProvider: SymbolProvider,
        clientPackage: String
    ): Pair<MethodSpec, MethodSpec> {
        val operationName = StringUtils.uncapitalize(operation.id.name)
        val outputSymbol =
            if (operation.output.isPresent) symbolProvider.toSymbol(model.expectShape(operation.output.get())) else null
        val outputType = outputSymbol?.toTypeName() ?: TypeName.VOID

        val docTrait = operation.getTrait(DocumentationTrait::class.java)
        val javadocString = if (docTrait.isPresent) docTrait.get().value + "\n\n" else ""

        // 1. Interface Method
        val interfaceMethod = MethodSpec.methodBuilder(operationName)
            .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
            .addJavadoc($$"$L", javadocString)
            .addException(ClassName.get("java.io", "IOException"))
            .returns(outputType)

        operation.input.ifPresent { inputId ->
            val inputShape = model.expectShape(inputId, StructureShape::class.java)
            interfaceMethod.addParameter(symbolProvider.toSymbol(inputShape).toTypeName(), "input")
            interfaceMethod.addJavadoc("@param input The request input.\n")
        }

        if (outputSymbol != null) {
            interfaceMethod.addJavadoc("@return The response output.\n")
        }
        interfaceMethod.addJavadoc("@throws java.io.IOException If the network call or serialization fails.\n")

        // 2. Implementation Method
        val implMethod = MethodSpec.methodBuilder(operationName)
            .addAnnotation(Override::class.java)
            .addModifiers(Modifier.PUBLIC)
            .addException(ClassName.get("java.io", "IOException"))
            .returns(outputType)

        val httpTrait = operation.getTrait(HttpTrait::class.java).orElseThrow {
            IllegalArgumentException("Operation ${operation.id} must have an @http trait for client generation")
        }

        implMethod.addStatement($$"$T uri = this.baseUrl + $S", String::class.java, httpTrait.uri.toString())

        operation.input.ifPresent { inputId ->
            val inputShape = model.expectShape(inputId, StructureShape::class.java)
            val inputSymbol = symbolProvider.toSymbol(inputShape)
            implMethod.addParameter(inputSymbol.toTypeName(), "input")

            val (metadataMembers, payloadMembers) = inputShape.getMetadataAndPayload()

            for (member in metadataMembers.filter { it.hasTrait(HttpLabelTrait::class.java) }) {
                val memberName = symbolProvider.toMemberName(member)
                implMethod.addStatement(
                    $$"uri = uri.replace($S, String.valueOf(input.$L()))",
                    "{${member.memberName}}", memberName
                )
            }

            val queryMembers = metadataMembers.filter { it.hasTrait(HttpQueryTrait::class.java) }
            if (queryMembers.isNotEmpty()) {
                val streamArgs = queryMembers.joinToString(",\n") { member ->
                    val queryTrait = member.expectTrait(HttpQueryTrait::class.java)
                    val memberName = symbolProvider.toMemberName(member)
                    "input.$memberName() != null ? \"${queryTrait.value}=\" + input.$memberName() : null"
                }

                implMethod.addStatement(
                    $$"String queryParams = $T.of(\n$L\n)\n.filter($T::nonNull)\n.collect($T.joining(\"&\"))",
                    java.util.stream.Stream::class.java,
                    streamArgs,
                    java.util.Objects::class.java,
                    java.util.stream.Collectors::class.java
                )

                implMethod.beginControlFlow("if (!queryParams.isEmpty())")
                implMethod.addStatement("uri += \"?\" + queryParams")
                implMethod.endControlFlow()
            }

            if (payloadMembers.size == 1) {
                val payloadMember = payloadMembers[0]
                implMethod.addStatement(
                    $$"byte[] body = codec.serialize(input.$L())",
                    symbolProvider.toMemberName(payloadMember)
                )
            } else if (payloadMembers.isNotEmpty()) {
                implMethod.addStatement("byte[] body = codec.serialize(input)")
            } else {
                implMethod.addStatement("byte[] body = new byte[0]")
            }
        }

        // Headers
        implMethod.addStatement(
            $$"$T<$T, $T<$T>> headers = new $T<>()",
            java.util.Map::class.java,
            String::class.java,
            java.util.List::class.java,
            String::class.java,
            java.util.HashMap::class.java
        )
        implMethod.addStatement(
            $$"headers.put($S, $T.of($S))",
            "Content-Type",
            java.util.List::class.java,
            "application/json"
        )

        // Input Headers
        operation.input.ifPresent { inputId ->
            val inputShape = model.expectShape(inputId, StructureShape::class.java)
            val (metadataMembers, _) = inputShape.getMetadataAndPayload()
            for (member in metadataMembers.filter { it.hasTrait(HttpHeaderTrait::class.java) }) {
                val headerTrait = member.expectTrait(HttpHeaderTrait::class.java)
                val memberName = symbolProvider.toMemberName(member)
                implMethod.beginControlFlow($$"if (input.$L() != null)", memberName)
                implMethod.addStatement(
                    $$"headers.put($S, $T.of(String.valueOf(input.$L())))",
                    headerTrait.value, java.util.List::class.java, memberName
                )
                implMethod.endControlFlow()
            }
        }

        implMethod.addStatement(
            $$"HttpRequest request = new HttpRequest($S, uri, headers, body)",
            httpTrait.method.uppercase()
        )
        implMethod.addStatement("HttpResponse response = transport.execute(request)")

        implMethod.beginControlFlow("if (response.statusCode() >= 200 && response.statusCode() < 300)")
        if (outputSymbol != null) {
            val outputShape = model.expectShape(operation.output.get(), StructureShape::class.java)
            val (metadataMembers, _) = outputShape.getMetadataAndPayload()

            if (metadataMembers.any { it.hasTrait(HttpHeaderTrait::class.java) }) {
                implMethod.addStatement(
                    $$"$T baseOutput = codec.deserialize(response.body(), $T.class)",
                    outputType,
                    outputType
                )

                // Extract headers and call constructor with all fields
                val constructorArgs = mutableListOf<String>()
                for (member in outputShape.allMembers.values) {
                    val memberName = symbolProvider.toMemberName(member)
                    if (member.hasTrait(HttpHeaderTrait::class.java)) {
                        val headerTrait = member.expectTrait(HttpHeaderTrait::class.java)
                        val memberSymbol = symbolProvider.toSymbol(member)
                        val typeName = memberSymbol.toTypeName()

                        // We define a local variable for the header
                        implMethod.addStatement($$"$T $L = null", typeName, memberName)
                        implMethod.beginControlFlow($$"if (response.headers().containsKey($S))", headerTrait.value)
                        implMethod.addStatement(
                            $$"String headerValue = response.headers().get($S).get(0)",
                            headerTrait.value
                        )

                        // Handle simple type conversion
                        when (typeName) {
                            TypeName.LONG, ClassName.get("java.lang", "Long") -> {
                                implMethod.addStatement(
                                    $$"$L = $T.parseLong(headerValue)",
                                    memberName,
                                    Long::class.javaObjectType
                                )
                            }

                            TypeName.INT, ClassName.get("java.lang", "Integer") -> {
                                implMethod.addStatement(
                                    $$"$L = $T.parseInt(headerValue)",
                                    memberName,
                                    Integer::class.javaObjectType
                                )
                            }

                            else -> {
                                implMethod.addStatement($$"$L = headerValue", memberName)
                            }
                        }
                        implMethod.endControlFlow()
                        constructorArgs.add(memberName)
                    } else {
                        constructorArgs.add("baseOutput.$memberName()")
                    }
                }
                implMethod.addStatement($$"return new $T($L)", outputType, constructorArgs.joinToString(", "))
            } else {
                implMethod.addStatement($$"return codec.deserialize(response.body(), $T.class)", outputType)
            }
        } else {
            implMethod.addStatement("return")
        }
        implMethod.nextControlFlow("else")
        implMethod.addStatement(
            $$"throw new RuntimeException($S + response.statusCode())",
            "Service returned error code: "
        )
        implMethod.endControlFlow()

        return Pair(interfaceMethod.build(), implMethod.build())
    }
}
