package com.wesleyhome.smithy.generator

/**
 * Built-in family ids that integrations can override via higher priority.
 */
object GeneratorFamilies {
	const val MODEL_STRUCTURES = "model:structures"
	const val MODEL_EXCEPTIONS = "model:exceptions"
	const val MODEL_ENUMS = "model:enums"
	const val MODEL_UNIONS = "model:unions"

	const val CLIENT_STRUCTURES = "client:structures"
	const val CLIENT_EXCEPTIONS = "client:exceptions"
	const val CLIENT_ENUMS = "client:enums"
	const val CLIENT_UNIONS = "client:unions"
	const val CLIENT_CORE = "client:core"
	const val CLIENT_SERVICE = "client:service"
	const val CLIENT_HTTP_TRANSPORT_JDK = "client:http-transport:jdk"
	const val CLIENT_HTTP_TRANSPORT_OKHTTP = "client:http-transport:okhttp"
	const val CLIENT_PROTOCOL_CODEC_JACKSON = "client:protocol-codec:jackson"
	const val CLIENT_PROTOCOL_CODEC_GSON = "client:protocol-codec:gson"

	const val SERVER_STRUCTURES = "server:structures"
	const val SERVER_EXCEPTIONS = "server:exceptions"
	const val SERVER_ENUMS = "server:enums"
	const val SERVER_UNIONS = "server:unions"
	const val SERVER_API = "server:api"
	const val SERVER_CONTROLLER = "server:controller"
	const val SERVER_EXCEPTION_HANDLER = "server:exception-handler"
	const val SERVER_FALLBACK_CONFIG = "server:fallback-config"
}
