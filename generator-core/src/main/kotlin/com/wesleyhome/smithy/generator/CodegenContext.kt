package com.wesleyhome.smithy.generator

import software.amazon.smithy.codegen.core.SymbolProvider
import software.amazon.smithy.model.Model

/**
 * Minimal language-agnostic context required by the generic integration lifecycle.
 */
interface CodegenContext<T : CodegenTarget> {
	val model: Model
	val symbolProvider: SymbolProvider
	val target: T
}
