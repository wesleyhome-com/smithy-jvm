package com.wesleyhome.smithy.generator

import com.palantir.javapoet.ArrayTypeName
import com.palantir.javapoet.ClassName
import com.palantir.javapoet.JavaFile
import com.palantir.javapoet.MethodSpec
import com.palantir.javapoet.ParameterizedTypeName
import com.palantir.javapoet.TypeName
import com.palantir.javapoet.TypeSpec
import com.palantir.javapoet.TypeVariableName
import software.amazon.smithy.codegen.core.SymbolProvider
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.shapes.ServiceShape
import javax.lang.model.element.Modifier

/**
 * Generates core HTTP and Protocol abstractions required by the Java Client.
 */
class JavaClientCoreAbstractionsGenerator(
    private val serializationLibrary: String = "none",
    private val httpClientLibrary: String = "jdk"
) : ShapeGenerator<ServiceShape> {
    override val shapeType: Class<ServiceShape> = ServiceShape::class.java

    override fun generate(shape: ServiceShape, model: Model, symbolProvider: SymbolProvider): ShapeGenerator.Result {
        val serviceSymbol = symbolProvider.toSymbol(shape)
        val baseNamespace = "${serviceSymbol.namespace}.client"

        val files = mutableListOf(
            generateHttpRequest(baseNamespace),
            generateHttpResponse(baseNamespace),
            generateHttpTransport(baseNamespace),
            generateProtocolCodec(baseNamespace)
        )

        // Generate specific adapters if configured
        if (httpClientLibrary == "jdk") {
            files.add(generateJdkHttpTransport(baseNamespace))
        } else if (httpClientLibrary == "okhttp") {
            files.add(generateOkHttpTransport(baseNamespace))
        }

        if (serializationLibrary == "jackson") {
            files.add(generateJacksonCodec(baseNamespace))
        } else if (serializationLibrary == "gson") {
            files.add(generateGsonCodec(baseNamespace))
        }

        return ShapeGenerator.Result(files.map { it.toGeneratedFile() })
    }

    private fun generateJdkHttpTransport(packageName: String): JavaFile {
        val httpClient = ClassName.get("java.net.http", "HttpClient")
        val httpClientBuilder = ClassName.get("java.net.http", "HttpClient", "Builder")
        val httpRequest = ClassName.get("java.net.http", "HttpRequest")
        val httpResponse = ClassName.get("java.net.http", "HttpResponse")
        val bodyHandlers = ClassName.get("java.net.http", "HttpResponse", "BodyHandlers")
        val uri = ClassName.get("java.net", "URI")
        val unaryOperator = ClassName.get("java.util.function", "UnaryOperator")

        val typeBuilder = TypeSpec.classBuilder("JdkHttpTransport")
            .addModifiers(Modifier.PUBLIC)
            .addSuperinterface(ClassName.get(packageName, "HttpTransport"))
            .addJavadoc("Implementation of HttpTransport using the JDK's HttpClient.\n")
            .addField(httpClient, "client", Modifier.PRIVATE, Modifier.FINAL)

            .addMethod(
                MethodSpec.constructorBuilder()
                    .addModifiers(Modifier.PUBLIC)
                    .addJavadoc("Creates a default JdkHttpTransport.\n")
                    .addStatement($$"this($T.identity())", unaryOperator)
                    .build()
            )

            .addMethod(
                MethodSpec.constructorBuilder()
                    .addModifiers(Modifier.PUBLIC)
                    .addJavadoc("Creates a JdkHttpTransport with a custom configured HttpClient.Builder.\n")
                    .addParameter(ParameterizedTypeName.get(unaryOperator, httpClientBuilder), "configurer")
                    .addStatement($$"$T builder = configurer.apply($T.newBuilder())", httpClientBuilder, httpClient)
                    .addStatement("this.client = builder.build()")
                    .build()
            )

            .addMethod(
                MethodSpec.methodBuilder("execute")
                    .addAnnotation(Override::class.java)
                    .addModifiers(Modifier.PUBLIC)
                    .addParameter(ClassName.get(packageName, "HttpRequest"), "request")
                    .returns(ClassName.get(packageName, "HttpResponse"))
                    .addException(ClassName.get("java.io", "IOException"))
                    .addStatement(
                        $$"$T.Builder builder = $T.newBuilder().uri($T.create(request.uri()))",
                        httpRequest,
                        httpRequest,
                        uri
                    )
                    .addStatement(
                        $$"builder.method(request.method(), $T.BodyPublishers.ofByteArray(request.body()))",
                        httpRequest
                    )
                    .addCode("\n")
                    .beginControlFlow("request.headers().forEach((name, values) ->")
                    .addStatement("values.forEach(value -> builder.header(name, value))")
                    .endControlFlow(")")
                    .addCode("\n")
                    .beginControlFlow("try")
                    .addStatement(
                        $$"$T<byte[]> response = client.send(builder.build(), $T.ofByteArray())",
                        httpResponse,
                        bodyHandlers
                    )
                    .addStatement("return new HttpResponse(response.statusCode(), response.headers().map(), response.body())")
                    .nextControlFlow($$"catch ($T e)", InterruptedException::class.java)
                    .addStatement($$"$T.currentThread().interrupt()", Thread::class.java)
                    .addStatement($$"throw new $T(\"Request interrupted\", e)", ClassName.get("java.io", "IOException"))
                    .endControlFlow()
                    .build()
            )

        return JavaFile.builder(packageName, typeBuilder.build()).build()
    }

    private fun generateOkHttpTransport(packageName: String): JavaFile {
        val okHttpClient = ClassName.get("okhttp3", "OkHttpClient")
        val okHttpBuilder = ClassName.get("okhttp3", "OkHttpClient", "Builder")
        val okHttpRequest = ClassName.get("okhttp3", "Request")
        val okHttpRequestBody = ClassName.get("okhttp3", "RequestBody")
        val okHttpResponse = ClassName.get("okhttp3", "Response")
        val mediaType = ClassName.get("okhttp3", "MediaType")
        val unaryOperator = ClassName.get("java.util.function", "UnaryOperator")

        val typeBuilder = TypeSpec.classBuilder("OkHttpTransport")
            .addModifiers(Modifier.PUBLIC)
            .addSuperinterface(ClassName.get(packageName, "HttpTransport"))
            .addJavadoc("Implementation of HttpTransport using OkHttp.\n")
            .addField(okHttpClient, "client", Modifier.PRIVATE, Modifier.FINAL)

            .addMethod(
                MethodSpec.constructorBuilder()
                    .addModifiers(Modifier.PUBLIC)
                    .addJavadoc("Creates a default OkHttpTransport.\n")
                    .addStatement($$"this($T.identity())", unaryOperator)
                    .build()
            )

            .addMethod(
                MethodSpec.constructorBuilder()
                    .addModifiers(Modifier.PUBLIC)
                    .addJavadoc("Creates an OkHttpTransport with a custom configured OkHttpClient.Builder.\n")
                    .addParameter(ParameterizedTypeName.get(unaryOperator, okHttpBuilder), "configurer")
                    .addStatement($$"this.client = configurer.apply(new $T.Builder()).build()", okHttpClient)
                    .build()
            )

            .addMethod(
                MethodSpec.methodBuilder("execute")
                    .addAnnotation(Override::class.java)
                    .addModifiers(Modifier.PUBLIC)
                    .addParameter(ClassName.get(packageName, "HttpRequest"), "request")
                    .returns(ClassName.get(packageName, "HttpResponse"))
                    .addException(ClassName.get("java.io", "IOException"))
                    .addStatement(
                        $$"$T.Builder builder = new $T.Builder().url(request.uri())",
                        okHttpRequest,
                        okHttpRequest
                    )
                    .addCode("\n")
                    .addStatement(
                        $$"$T body = (request.body() == null || request.body().length == 0) ? null : $T.create(request.body(), $T.parse(\"application/json\"))",
                        okHttpRequestBody, okHttpRequestBody, mediaType
                    )
                    .addStatement("builder.method(request.method(), body)")
                    .addCode("\n")
                    .beginControlFlow("request.headers().forEach((name, values) ->")
                    .addStatement("values.forEach(value -> builder.addHeader(name, value))")
                    .endControlFlow(")")
                    .addCode("\n")
                    .beginControlFlow($$"try ($T response = client.newCall(builder.build()).execute())", okHttpResponse)
                    .addStatement("byte[] responseBody = response.body() != null ? response.body().bytes() : new byte[0]")
                    .addStatement("return new HttpResponse(response.code(), response.headers().toMultimap(), responseBody)")
                    .endControlFlow()
                    .build()
            )

        return JavaFile.builder(packageName, typeBuilder.build()).build()
    }

    private fun generateJacksonCodec(packageName: String): JavaFile {
        val objectMapper = ClassName.get("com.fasterxml.jackson.databind", "ObjectMapper")
        val unaryOperator = ClassName.get("java.util.function", "UnaryOperator")

        val typeBuilder = TypeSpec.classBuilder("JacksonCodec")
            .addModifiers(Modifier.PUBLIC)
            .addSuperinterface(ClassName.get(packageName, "ProtocolCodec"))
            .addField(objectMapper, "mapper", Modifier.PRIVATE, Modifier.FINAL)

            .addMethod(
                MethodSpec.constructorBuilder()
                    .addModifiers(Modifier.PUBLIC)
                    .addJavadoc("Creates a default JacksonCodec.\n")
                    .addStatement($$"this($T.identity())", unaryOperator)
                    .build()
            )

            .addMethod(
                MethodSpec.constructorBuilder()
                    .addModifiers(Modifier.PUBLIC)
                    .addJavadoc("Creates a JacksonCodec with a custom configured ObjectMapper.\n")
                    .addParameter(ParameterizedTypeName.get(unaryOperator, objectMapper), "configurer")
                    .addStatement($$"this.mapper = configurer.apply(new $T())", objectMapper)
                    .build()
            )

            .addMethod(
                MethodSpec.methodBuilder("serialize")
                    .addAnnotation(Override::class.java)
                    .addModifiers(Modifier.PUBLIC)
                    .addParameter(Object::class.java, "input")
                    .returns(ArrayTypeName.of(TypeName.BYTE))
                    .addException(ClassName.get("java.io", "IOException"))
                    .addStatement("return mapper.writeValueAsBytes(input)")
                    .build()
            )
            .addMethod(
                MethodSpec.methodBuilder("deserialize")
                    .addAnnotation(Override::class.java)
                    .addModifiers(Modifier.PUBLIC)
                    .addTypeVariable(TypeVariableName.get("T"))
                    .addParameter(ArrayTypeName.of(TypeName.BYTE), "payload")
                    .addParameter(
                        ParameterizedTypeName.get(
                            ClassName.get("java.lang", "Class"),
                            TypeVariableName.get("T")
                        ), "clazz"
                    )
                    .returns(TypeVariableName.get("T"))
                    .addException(ClassName.get("java.io", "IOException"))
                    .addStatement("return mapper.readValue(payload, clazz)")
                    .build()
            )

        return JavaFile.builder(packageName, typeBuilder.build()).build()
    }

    private fun generateGsonCodec(packageName: String): JavaFile {
        val gson = ClassName.get("com.google.gson", "Gson")
        val gsonBuilder = ClassName.get("com.google.gson", "GsonBuilder")
        val unaryOperator = ClassName.get("java.util.function", "UnaryOperator")

        val typeBuilder = TypeSpec.classBuilder("GsonCodec")
            .addModifiers(Modifier.PUBLIC)
            .addSuperinterface(ClassName.get(packageName, "ProtocolCodec"))
            .addField(gson, "gson", Modifier.PRIVATE, Modifier.FINAL)

            .addMethod(
                MethodSpec.constructorBuilder()
                    .addModifiers(Modifier.PUBLIC)
                    .addJavadoc("Creates a default GsonCodec.\n")
                    .addStatement($$"this($T.identity())", unaryOperator)
                    .build()
            )

            .addMethod(
                MethodSpec.constructorBuilder()
                    .addModifiers(Modifier.PUBLIC)
                    .addJavadoc("Creates a GsonCodec with a custom configured GsonBuilder.\n")
                    .addParameter(ParameterizedTypeName.get(unaryOperator, gsonBuilder), "configurer")
                    .addStatement($$"this.gson = configurer.apply(new $T()).create()", gsonBuilder)
                    .build()
            )

            .addMethod(
                MethodSpec.methodBuilder("serialize")
                    .addAnnotation(Override::class.java)
                    .addModifiers(Modifier.PUBLIC)
                    .addParameter(Object::class.java, "input")
                    .returns(ArrayTypeName.of(TypeName.BYTE))
                    .addException(ClassName.get("java.io", "IOException"))
                    .addStatement("return gson.toJson(input).getBytes(java.nio.charset.StandardCharsets.UTF_8)")
                    .build()
            )
            .addMethod(
                MethodSpec.methodBuilder("deserialize")
                    .addAnnotation(Override::class.java)
                    .addModifiers(Modifier.PUBLIC)
                    .addTypeVariable(TypeVariableName.get("T"))
                    .addParameter(ArrayTypeName.of(TypeName.BYTE), "payload")
                    .addParameter(
                        ParameterizedTypeName.get(
                            ClassName.get("java.lang", "Class"),
                            TypeVariableName.get("T")
                        ), "clazz"
                    )
                    .returns(TypeVariableName.get("T"))
                    .addException(ClassName.get("java.io", "IOException"))
                    .addStatement("return gson.fromJson(new String(payload, java.nio.charset.StandardCharsets.UTF_8), clazz)")
                    .build()
            )

        return JavaFile.builder(packageName, typeBuilder.build()).build()
    }

    private fun generateHttpRequest(packageName: String): JavaFile {
        val typeBuilder = TypeSpec.recordBuilder("HttpRequest")
            .addModifiers(Modifier.PUBLIC)
            .addJavadoc("Represents an outgoing HTTP request.\n\n")
            .addJavadoc("@param method The HTTP method (e.g., GET, POST).\n")
            .addJavadoc("@param uri The full URI of the request.\n")
            .addJavadoc("@param headers The HTTP headers.\n")
            .addJavadoc("@param body The request body payload as a byte array.\n")
            .recordConstructor(
                MethodSpec.constructorBuilder()
                    .addParameter(String::class.java, "method")
                    .addParameter(String::class.java, "uri")
                    .addParameter(
                        ParameterizedTypeName.get(
                            ClassName.get("java.util", "Map"),
                            ClassName.get("java.lang", "String"),
                            ParameterizedTypeName.get(
                                ClassName.get("java.util", "List"),
                                ClassName.get("java.lang", "String")
                            )
                        ), "headers"
                    )
                    .addParameter(ArrayTypeName.of(TypeName.BYTE), "body")
                    .build()
            )

        return JavaFile.builder(packageName, typeBuilder.build()).build()
    }

    private fun generateHttpResponse(packageName: String): JavaFile {
        val typeBuilder = TypeSpec.recordBuilder("HttpResponse")
            .addModifiers(Modifier.PUBLIC)
            .addJavadoc("Represents an incoming HTTP response.\n\n")
            .addJavadoc("@param statusCode The HTTP status code.\n")
            .addJavadoc("@param headers The HTTP response headers.\n")
            .addJavadoc("@param body The response body payload as a byte array.\n")
            .recordConstructor(
                MethodSpec.constructorBuilder()
                    .addParameter(TypeName.INT, "statusCode")
                    .addParameter(
                        ParameterizedTypeName.get(
                            ClassName.get("java.util", "Map"),
                            ClassName.get("java.lang", "String"),
                            ParameterizedTypeName.get(
                                ClassName.get("java.util", "List"),
                                ClassName.get("java.lang", "String")
                            )
                        ), "headers"
                    )
                    .addParameter(ArrayTypeName.of(TypeName.BYTE), "body")
                    .build()
            )

        return JavaFile.builder(packageName, typeBuilder.build()).build()
    }

    private fun generateHttpTransport(packageName: String): JavaFile {
        val typeBuilder = TypeSpec.interfaceBuilder("HttpTransport")
            .addModifiers(Modifier.PUBLIC)
            .addJavadoc("Pluggable HTTP transport layer.\n")
            .addMethod(
                MethodSpec.methodBuilder("execute")
                    .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                    .addJavadoc("Executes an HTTP request and returns the response.\n\n")
                    .addJavadoc("@param request The HTTP request to execute.\n")
                    .addJavadoc("@return The HTTP response.\n")
                    .addJavadoc("@throws java.io.IOException If an I/O error occurs during the execution.\n")
                    .addParameter(ClassName.get(packageName, "HttpRequest"), "request")
                    .returns(ClassName.get(packageName, "HttpResponse"))
                    .addException(ClassName.get("java.io", "IOException"))
                    .build()
            )

        return JavaFile.builder(packageName, typeBuilder.build()).build()
    }

    private fun generateProtocolCodec(packageName: String): JavaFile {
        val typeBuilder = TypeSpec.interfaceBuilder("ProtocolCodec")
            .addModifiers(Modifier.PUBLIC)
            .addJavadoc("Pluggable serialization layer for encoding and decoding protocol messages.\n")
            .addMethod(
                MethodSpec.methodBuilder("serialize")
                    .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                    .addJavadoc("Serializes the given input object into a byte array.\n\n")
                    .addJavadoc("@param input The object to serialize.\n")
                    .addJavadoc("@return The serialized byte array.\n")
                    .addJavadoc("@throws java.io.IOException If serialization fails.\n")
                    .addParameter(ClassName.get("java.lang", "Object"), "input")
                    .returns(ArrayTypeName.of(TypeName.BYTE))
                    .addException(ClassName.get("java.io", "IOException"))
                    .build()
            )
            .addMethod(
                MethodSpec.methodBuilder("deserialize")
                    .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                    .addJavadoc("Deserializes the given byte array payload into the specified class.\n\n")
                    .addJavadoc("@param <T> The type of the object to deserialize into.\n")
                    .addJavadoc("@param payload The byte array payload to deserialize.\n")
                    .addJavadoc("@param clazz The class of the object to deserialize into.\n")
                    .addJavadoc("@return The deserialized object.\n")
                    .addJavadoc("@throws java.io.IOException If deserialization fails.\n")
                    .addTypeVariable(TypeVariableName.get("T"))
                    .addParameter(ArrayTypeName.of(TypeName.BYTE), "payload")
                    .addParameter(
                        ParameterizedTypeName.get(
                            ClassName.get("java.lang", "Class"),
                            TypeVariableName.get("T")
                        ), "clazz"
                    )
                    .returns(TypeVariableName.get("T"))
                    .addException(ClassName.get("java.io", "IOException"))
                    .build()
            )

        return JavaFile.builder(packageName, typeBuilder.build()).build()
    }
}
