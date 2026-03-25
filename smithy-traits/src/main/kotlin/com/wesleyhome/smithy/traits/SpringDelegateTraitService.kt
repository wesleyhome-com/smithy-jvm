package com.wesleyhome.smithy.traits

import software.amazon.smithy.model.node.Node
import software.amazon.smithy.model.shapes.ShapeId
import software.amazon.smithy.model.traits.AnnotationTrait
import software.amazon.smithy.model.traits.TraitDefinition
import software.amazon.smithy.model.traits.TraitService

class SpringDelegateTraitService : TraitService {
    override fun getShapeId(): ShapeId {
        return SpringDelegateTrait.ID
    }

    override fun createTrait(target: ShapeId, node: Node): SpringDelegateTrait {
        val value = node.asStringNode().map { it.value }.orElse("")
        return SpringDelegateTrait.builder().value(value).sourceLocation(node.sourceLocation).build()
    }
}
