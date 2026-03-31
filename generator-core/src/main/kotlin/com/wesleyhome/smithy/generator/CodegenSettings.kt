package com.wesleyhome.smithy.generator

import software.amazon.smithy.model.node.ObjectNode

/**
 * Typed access wrapper for Smithy plugin settings.
 */
data class CodegenSettings(private val node: ObjectNode) {

	fun requireString(name: String): String {
		return node.getStringMember(name).orElseThrow {
			IllegalArgumentException("Missing required '$name' configuration in smithy-build.json")
		}.value
	}

	fun getString(name: String): String? = node.getStringMember(name).map { it.value }.orElse(null)

	companion object {
		fun from(node: ObjectNode): CodegenSettings = CodegenSettings(node)
	}
}
