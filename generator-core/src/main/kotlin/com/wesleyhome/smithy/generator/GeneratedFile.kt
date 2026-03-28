package com.wesleyhome.smithy.generator

/**
 * A generic representation of a file to be written to disk.
 * This decouples the core plugin from language-specific AST builders like JavaPoet or KotlinPoet.
 */
data class GeneratedFile(
	val path: String,
	val content: String
)
