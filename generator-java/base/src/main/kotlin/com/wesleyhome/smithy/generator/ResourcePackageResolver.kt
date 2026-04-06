package com.wesleyhome.smithy.generator

import software.amazon.smithy.model.Model
import software.amazon.smithy.model.knowledge.TopDownIndex
import software.amazon.smithy.model.shapes.ListShape
import software.amazon.smithy.model.shapes.MapShape
import software.amazon.smithy.model.shapes.MemberShape
import software.amazon.smithy.model.shapes.OperationShape
import software.amazon.smithy.model.shapes.ResourceShape
import software.amazon.smithy.model.shapes.ServiceShape
import software.amazon.smithy.model.shapes.Shape
import software.amazon.smithy.model.shapes.ShapeId
import software.amazon.smithy.model.shapes.ShapeType
import software.amazon.smithy.model.traits.TagsTrait

/**
 * Resolves package domain keys using resource-first grouping with tag fallback.
 *
 * Nested resources are collapsed to their root domain package key so generated code
 * follows package-by-domain first (e.g., "...patron.api", "...patron.model").
 */
class ResourcePackageResolver(
	private val model: Model,
	private val serviceShape: ServiceShape
) {
	private val containedOperationIds: Set<ShapeId> =
		TopDownIndex.of(model).getContainedOperations(serviceShape).map { it.id }.toSet()

	private val operationDomainKeys: Map<ShapeId, String> = buildOperationDomainKeys()
	private val shapeDomainKeys: Map<ShapeId, Set<String>> = buildShapeDomainKeys()

	fun domainKeyForShape(shape: Shape): String? {
		if (shape.type == ShapeType.OPERATION) {
			return domainKeyForOperation(shape.asOperationShape().orElse(null))
		}

		val keys = shapeDomainKeys[shape.id].orEmpty()
		if (keys.isNotEmpty()) {
			return keys.sorted().first()
		}

		return tagDomainKey(shape)
	}

	fun domainKeyForOperation(operation: OperationShape?): String? {
		if (operation == null) {
			return null
		}
		return operationDomainKeys[operation.id] ?: tagDomainKey(operation)
	}

	private fun buildOperationDomainKeys(): Map<ShapeId, String> {
		val keys = linkedMapOf<ShapeId, String>()
		for (rootResourceId in serviceShape.resources) {
			val rootResource = model.expectShape(rootResourceId, ResourceShape::class.java)
			val rootDomainKey = normalizePackageSegment(rootResource.id.name)
			visitResource(rootResource, rootDomainKey, keys)
		}
		return keys
	}

	private fun visitResource(resource: ResourceShape, rootDomainKey: String, keys: MutableMap<ShapeId, String>) {
		resource.allOperations.forEach { operationId ->
			if (operationId in containedOperationIds) {
				keys.putIfAbsent(operationId, rootDomainKey)
			}
		}

		resource.resources.forEach { childResourceId ->
			val childResource = model.expectShape(childResourceId, ResourceShape::class.java)
			visitResource(childResource, rootDomainKey, keys)
		}
	}

	private fun buildShapeDomainKeys(): Map<ShapeId, Set<String>> {
		val result = mutableMapOf<ShapeId, MutableSet<String>>()

		for (operationId in containedOperationIds) {
			val operation = model.expectShape(operationId, OperationShape::class.java)
			val key = domainKeyForOperation(operation) ?: continue
			val visited = linkedSetOf<ShapeId>()

			operation.input.ifPresent { collectReferencedShapes(it, visited) }
			operation.output.ifPresent { collectReferencedShapes(it, visited) }
			operation.errors.forEach { collectReferencedShapes(it, visited) }

			visited
				.filter { shapeId ->
					shapeId.namespace != "smithy.api"
						&& model.expectShape(shapeId).type != ShapeType.OPERATION
						&& model.expectShape(shapeId).type != ShapeType.SERVICE
				}
				.forEach { shapeId ->
					result.getOrPut(shapeId) { linkedSetOf() }.add(key)
				}
		}

		return result.mapValues { it.value.toSet() }
	}

	private fun collectReferencedShapes(shapeId: ShapeId, visited: MutableSet<ShapeId>) {
		if (!visited.add(shapeId)) {
			return
		}

		if (shapeId.namespace == "smithy.api") {
			return
		}

		val shape = model.expectShape(shapeId)
		when (shape.type) {
			ShapeType.MEMBER -> {
				val member = shape as MemberShape
				collectReferencedShapes(member.target, visited)
			}

			ShapeType.STRUCTURE, ShapeType.UNION -> {
				shape.members().forEach { member -> collectReferencedShapes(member.target, visited) }
			}

			ShapeType.LIST -> {
				val listShape = shape as ListShape
				collectReferencedShapes(listShape.member.target, visited)
			}

			ShapeType.MAP -> {
				val mapShape = shape as MapShape
				collectReferencedShapes(mapShape.key.target, visited)
				collectReferencedShapes(mapShape.value.target, visited)
			}

			else -> Unit
		}
	}

	private fun tagDomainKey(shape: Shape): String? {
		val tag = shape.getTrait(TagsTrait::class.java)
			.map { it.values.firstOrNull() }
			.orElse(null)
			?: return null
		return normalizePackageSegment(tag)
	}

	private fun normalizePackageSegment(value: String): String {
		var raw = value.lowercase().replace(Regex("[^a-z0-9_]"), "")
		if (raw.endsWith("resource") && raw.length > "resource".length) {
			raw = raw.removeSuffix("resource")
		}
		if (raw.isBlank()) {
			return "domain"
		}
		return if (raw[0].isDigit()) "_$raw" else raw
	}
}
