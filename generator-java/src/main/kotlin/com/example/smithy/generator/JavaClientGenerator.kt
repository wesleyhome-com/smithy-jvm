package com.example.smithy.generator

import com.palantir.javapoet.*
import software.amazon.smithy.codegen.core.SymbolProvider
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.shapes.ServiceShape
import software.amazon.smithy.model.shapes.OperationShape
import software.amazon.smithy.model.shapes.StructureShape
import software.amazon.smithy.model.shapes.Shape
import software.amazon.smithy.model.knowledge.TopDownIndex
import software.amazon.smithy.model.traits.*
import software.amazon.smithy.utils.StringUtils
import javax.lang.model.element.Modifier

/**
 * Generates a production-ready Java Client for a Smithy Service, using the Builder pattern.
 */
class JavaClientGenerator(
    private val serializationLibrary: String = "none",
    private val httpClientLibrary: String = "jdk"
) : ShapeGenerator<ServiceShape> {
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
            .addJavadoc("Internal implementation of {@link \$L}.\n", interfaceName)

        // 1. Implementation Fields & Constructor
        implementationBuilder.addField(ClassName.get(packageName, "HttpTransport"), "transport", Modifier.PRIVATE, Modifier.FINAL)
        implementationBuilder.addField(ClassName.get(packageName, "ProtocolCodec"), "codec", Modifier.PRIVATE, Modifier.FINAL)
        implementationBuilder.addField(String::class.java, "baseUrl", Modifier.PRIVATE, Modifier.FINAL)

        implementationBuilder.addMethod(MethodSpec.constructorBuilder()
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
            .build())

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
        interfaceBuilder.addMethod(MethodSpec.methodBuilder("builder")
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
            .addJavadoc("Creates a new builder for configuring the client.\n\n")
            .addJavadoc("@return A new builder instance.\n")
            .returns(ClassName.get(packageName, interfaceName, "Builder"))
            .addStatement("return new \$T()", ClassName.get(packageName, "Default${interfaceName}Builder"))
            .build())
            
        // 4. Convenience create method (if defaults exist)
        if (httpClientLibrary != "none" && serializationLibrary != "none") {
            interfaceBuilder.addMethod(MethodSpec.methodBuilder("create")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addJavadoc("Creates a client with default transport and codec settings for the given base URL.\n\n")
                .addJavadoc("@param baseUrl The base URL of the service.\n")
                .addJavadoc("@return A configured client instance.\n")
                .addParameter(String::class.java, "baseUrl")
                .returns(ClassName.get(packageName, interfaceName))
                .addStatement("return builder().baseUrl(baseUrl).build()")
                .build())
        }

        val files = listOf(
            JavaFile.builder(packageName, interfaceBuilder.build()).build(),
            JavaFile.builder(packageName, implementationBuilder.build()).build(),
            JavaFile.builder(packageName, builderImpl).build()
        )

        return ShapeGenerator.Result(files.map { it.toGeneratedFile() })
    }

    private fun generateBuilderInterface(packageName: String, interfaceName: String): TypeSpec {
        val builderInterfaceType = ClassName.get(packageName, interfaceName, "Builder")
        val unaryOperator = ClassName.get("java.util.function", "UnaryOperator")
        
        val builder = TypeSpec.interfaceBuilder("Builder")
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
            .addJavadoc("Builder for configuring and creating a {@link \$L}.\n", interfaceName)
            .addMethod(MethodSpec.methodBuilder("baseUrl")
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .addJavadoc("Sets the base URL for the service.\n\n")
                .addJavadoc("@param baseUrl The base URL.\n")
                .addJavadoc("@return This builder.\n")
                .addParameter(String::class.java, "baseUrl")
                .returns(builderInterfaceType)
                .build())
            .addMethod(MethodSpec.methodBuilder("transport")
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .addJavadoc("Sets the underlying HTTP transport implementation.\n\n")
                .addJavadoc("@param transport The transport implementation.\n")
                .addJavadoc("@return This builder.\n")
                .addParameter(ClassName.get(packageName, "HttpTransport"), "transport")
                .returns(builderInterfaceType)
                .build())
            .addMethod(MethodSpec.methodBuilder("codec")
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .addJavadoc("Sets the serialization codec implementation.\n\n")
                .addJavadoc("@param codec The codec implementation.\n")
                .addJavadoc("@return This builder.\n")
                .addParameter(ClassName.get(packageName, "ProtocolCodec"), "codec")
                .returns(builderInterfaceType)
                .build())

        // Convenience configurers
        if (httpClientLibrary == "jdk") {
            builder.addMethod(MethodSpec.methodBuilder("jdkHttpClient")
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .addJavadoc("Configures the default JDK HttpClient via a UnaryOperator.\n\n")
                .addJavadoc("@param configurer A function that takes the default builder and returns a configured one.\n")
                .addJavadoc("@return This builder.\n")
                .addParameter(ParameterizedTypeName.get(unaryOperator, ClassName.get("java.net.http", "HttpClient", "Builder")), "configurer")
                .returns(builderInterfaceType)
                .build())
        } else if (httpClientLibrary == "okhttp") {
            builder.addMethod(MethodSpec.methodBuilder("okHttp")
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .addJavadoc("Configures the default OkHttp client via a UnaryOperator.\n\n")
                .addJavadoc("@param configurer A function that takes the default builder and returns a configured one.\n")
                .addJavadoc("@return This builder.\n")
                .addParameter(ParameterizedTypeName.get(unaryOperator, ClassName.get("okhttp3", "OkHttpClient", "Builder")), "configurer")
                .returns(builderInterfaceType)
                .build())
        }

        if (serializationLibrary == "jackson") {
            builder.addMethod(MethodSpec.methodBuilder("jackson")
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .addJavadoc("Configures the default Jackson ObjectMapper via a UnaryOperator.\n\n")
                .addJavadoc("@param configurer A function that takes the default mapper and returns a configured one.\n")
                .addJavadoc("@return This builder.\n")
                .addParameter(ParameterizedTypeName.get(unaryOperator, ClassName.get("com.fasterxml.jackson.databind", "ObjectMapper")), "configurer")
                .returns(builderInterfaceType)
                .build())
        } else if (serializationLibrary == "gson") {
            builder.addMethod(MethodSpec.methodBuilder("gson")
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .addJavadoc("Configures the default GSON Builder via a UnaryOperator.\n\n")
                .addJavadoc("@param configurer A function that takes the default builder and returns a configured one.\n")
                .addJavadoc("@return This builder.\n")
                .addParameter(ParameterizedTypeName.get(unaryOperator, ClassName.get("com.google.gson", "GsonBuilder")), "configurer")
                .returns(builderInterfaceType)
                .build())
        }

        builder.addMethod(MethodSpec.methodBuilder("build")
            .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
            .addJavadoc("Builds and returns the configured client.\n\n")
            .addJavadoc("@return The client instance.\n")
            .returns(ClassName.get(packageName, interfaceName))
            .build())

        return builder.build()
    }

    private fun generateBuilderImplementation(packageName: String, interfaceName: String, implementationName: String): TypeSpec {
        val builderImplName = "Default${interfaceName}Builder"
        val builderInterfaceType = ClassName.get(packageName, interfaceName, "Builder")
        val unaryOperator = ClassName.get("java.util.function", "UnaryOperator")

        val builder = TypeSpec.classBuilder(builderImplName)
            .addModifiers(Modifier.FINAL) // package-private
            .addSuperinterface(builderInterfaceType)
            .addJavadoc("Internal builder implementation.\n")
            .addField(String::class.java, "baseUrl", Modifier.PRIVATE)
            .addField(ClassName.get(packageName, "HttpTransport"), "transport", Modifier.PRIVATE)
            .addField(ClassName.get(packageName, "ProtocolCodec"), "codec", Modifier.PRIVATE)

        builder.addMethod(MethodSpec.methodBuilder("baseUrl")
            .addAnnotation(Override::class.java)
            .addModifiers(Modifier.PUBLIC)
            .addParameter(String::class.java, "baseUrl")
            .returns(builderInterfaceType)
            .addStatement("this.baseUrl = baseUrl")
            .addStatement("return this")
            .build())

        builder.addMethod(MethodSpec.methodBuilder("transport")
            .addAnnotation(Override::class.java)
            .addModifiers(Modifier.PUBLIC)
            .addParameter(ClassName.get(packageName, "HttpTransport"), "transport")
            .returns(builderInterfaceType)
            .addStatement("this.transport = transport")
            .addStatement("return this")
            .build())

        builder.addMethod(MethodSpec.methodBuilder("codec")
            .addAnnotation(Override::class.java)
            .addModifiers(Modifier.PUBLIC)
            .addParameter(ClassName.get(packageName, "ProtocolCodec"), "codec")
            .returns(builderInterfaceType)
            .addStatement("this.codec = codec")
            .addStatement("return this")
            .build())

        // Implementation of convenience configurers
        if (httpClientLibrary == "jdk") {
            builder.addMethod(MethodSpec.methodBuilder("jdkHttpClient")
                .addAnnotation(Override::class.java)
                .addModifiers(Modifier.PUBLIC)
                .addParameter(ParameterizedTypeName.get(unaryOperator, ClassName.get("java.net.http", "HttpClient", "Builder")), "configurer")
                .returns(builderInterfaceType)
                .addStatement("this.transport = new \$T(configurer)", ClassName.get(packageName, "JdkHttpTransport"))
                .addStatement("return this")
                .build())
        } else if (httpClientLibrary == "okhttp") {
            builder.addMethod(MethodSpec.methodBuilder("okHttp")
                .addAnnotation(Override::class.java)
                .addModifiers(Modifier.PUBLIC)
                .addParameter(ParameterizedTypeName.get(unaryOperator, ClassName.get("okhttp3", "OkHttpClient", "Builder")), "configurer")
                .returns(builderInterfaceType)
                .addStatement("this.transport = new \$T(configurer)", ClassName.get(packageName, "OkHttpTransport"))
                .addStatement("return this")
                .build())
        }

        if (serializationLibrary == "jackson") {
            builder.addMethod(MethodSpec.methodBuilder("jackson")
                .addAnnotation(Override::class.java)
                .addModifiers(Modifier.PUBLIC)
                .addParameter(ParameterizedTypeName.get(unaryOperator, ClassName.get("com.fasterxml.jackson.databind", "ObjectMapper")), "configurer")
                .returns(builderInterfaceType)
                .addStatement("this.codec = new \$T(configurer)", ClassName.get(packageName, "JacksonCodec"))
                .addStatement("return this")
                .build())
        } else if (serializationLibrary == "gson") {
            builder.addMethod(MethodSpec.methodBuilder("gson")
                .addAnnotation(Override::class.java)
                .addModifiers(Modifier.PUBLIC)
                .addParameter(ParameterizedTypeName.get(unaryOperator, ClassName.get("com.google.gson", "GsonBuilder")), "configurer")
                .returns(builderInterfaceType)
                .addStatement("this.codec = new \$T(configurer)", ClassName.get(packageName, "GsonCodec"))
                .addStatement("return this")
                .build())
        }

        val buildMethod = MethodSpec.methodBuilder("build")
            .addAnnotation(Override::class.java)
            .addModifiers(Modifier.PUBLIC)
            .returns(ClassName.get(packageName, interfaceName))
            .beginControlFlow("if (this.baseUrl == null)")
            .addStatement("throw new \$T(\"Base URL must be configured\")", IllegalStateException::class.java)
            .endControlFlow()

        buildMethod.beginControlFlow("if (this.transport == null)")
        if (httpClientLibrary == "jdk") {
            buildMethod.addStatement("this.transport = new \$T()", ClassName.get(packageName, "JdkHttpTransport"))
        } else if (httpClientLibrary == "okhttp") {
            buildMethod.addStatement("this.transport = new \$T()", ClassName.get(packageName, "OkHttpTransport"))
        } else {
            buildMethod.addStatement("throw new \$T(\"No HttpTransport configured and no default library specified.\")", IllegalStateException::class.java)
        }
        buildMethod.endControlFlow()

        buildMethod.beginControlFlow("if (this.codec == null)")
        if (serializationLibrary == "jackson") {
            buildMethod.addStatement("this.codec = new \$T()", ClassName.get(packageName, "JacksonCodec"))
        } else if (serializationLibrary == "gson") {
            buildMethod.addStatement("this.codec = new \$T()", ClassName.get(packageName, "GsonCodec"))
        } else {
            buildMethod.addStatement("throw new \$T(\"No ProtocolCodec configured and no default library specified.\")", IllegalStateException::class.java)
        }
        buildMethod.endControlFlow()

        buildMethod.addStatement("return new \$T(this.transport, this.codec, this.baseUrl)", ClassName.get(packageName, implementationName))

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
        val outputSymbol = if (operation.output.isPresent) symbolProvider.toSymbol(model.expectShape(operation.output.get())) else null
        val outputType = outputSymbol?.toTypeName() ?: TypeName.VOID

        val docTrait = operation.getTrait(DocumentationTrait::class.java)
        val javadocString = if (docTrait.isPresent) docTrait.get().value + "\n\n" else ""

        // 1. Interface Method
        val interfaceMethod = MethodSpec.methodBuilder(operationName)
            .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
            .addJavadoc("\$L", javadocString)
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
        
        implMethod.addStatement("\$T uri = this.baseUrl + \$S", String::class.java, httpTrait.uri.toString())

        operation.input.ifPresent { inputId ->
            val inputShape = model.expectShape(inputId, StructureShape::class.java)
            val inputSymbol = symbolProvider.toSymbol(inputShape)
            implMethod.addParameter(inputSymbol.toTypeName(), "input")

            val (metadataMembers, payloadMembers) = inputShape.getMetadataAndPayload()

            for (member in metadataMembers.filter { it.hasTrait(HttpLabelTrait::class.java) }) {
                val memberName = symbolProvider.toMemberName(member)
                implMethod.addStatement("uri = uri.replace(\$S, String.valueOf(input.\$L()))", 
                    "{${member.memberName}}", memberName)
            }

            val queryMembers = metadataMembers.filter { it.hasTrait(HttpQueryTrait::class.java) }
            if (queryMembers.isNotEmpty()) {
                implMethod.addStatement("StringBuilder query = new StringBuilder()")
                for (member in queryMembers) {
                    val queryTrait = member.expectTrait(HttpQueryTrait::class.java)
                    val memberName = symbolProvider.toMemberName(member)
                    implMethod.beginControlFlow("if (input.\$L() != null)", memberName)
                    implMethod.addStatement("query.append(query.length() == 0 ? \"?\" : \"&\")")
                    implMethod.addStatement("query.append(\$S).append(\"=\").append(input.\$L())", 
                        queryTrait.value, memberName)
                    implMethod.endControlFlow()
                }
                implMethod.addStatement("uri += query.toString()")
            }

            if (payloadMembers.size == 1) {
                val payloadMember = payloadMembers[0]
                implMethod.addStatement("byte[] body = codec.serialize(input.\$L())", symbolProvider.toMemberName(payloadMember))
            } else if (payloadMembers.isNotEmpty()) {
                implMethod.addStatement("byte[] body = codec.serialize(input)")
            } else {
                implMethod.addStatement("byte[] body = new byte[0]")
            }
        } ?: run {
            implMethod.addStatement("byte[] body = new byte[0]")
        }

        implMethod.addStatement("\$T<\$T, \$T<\$T>> headers = new \$T<>()", 
            java.util.Map::class.java, String::class.java, java.util.List::class.java, String::class.java, java.util.HashMap::class.java)
        implMethod.addStatement("headers.put(\$S, \$T.asList(\$S))", "Content-Type", java.util.Arrays::class.java, "application/json")

        implMethod.addStatement("HttpRequest request = new HttpRequest(\$S, uri, headers, body)", httpTrait.method.uppercase())
        implMethod.addStatement("HttpResponse response = transport.execute(request)")

        implMethod.beginControlFlow("if (response.statusCode() >= 200 && response.statusCode() < 300)")
        if (outputSymbol != null) {
            implMethod.addStatement("return codec.deserialize(response.body(), \$T.class)", outputType)
        } else {
            implMethod.addStatement("return")
        }
        implMethod.nextControlFlow("else")
        implMethod.addStatement("throw new RuntimeException(\$S + response.statusCode())", "Service returned error code: ")
        implMethod.endControlFlow()

        return Pair(interfaceMethod.build(), implMethod.build())
    }
}
