package com.example.smithy.traits

import software.amazon.smithy.model.node.Node
import software.amazon.smithy.model.node.ObjectNode
import software.amazon.smithy.model.shapes.ShapeId
import software.amazon.smithy.model.traits.AbstractTrait
import software.amazon.smithy.model.traits.AbstractTraitBuilder
import software.amazon.smithy.model.traits.Trait
import software.amazon.smithy.utils.ToSmithyBuilder

/**
 * Indicates that a service should use a delegate pattern for its implementation.
 */
class SpringDelegateTrait(builder: Builder) : AbstractTrait(ID, builder.sourceLocation), ToSmithyBuilder<SpringDelegateTrait> {

    val value: String = builder.value ?: ""

    override fun createNode(): Node {
        return Node.from(value)
    }

    override fun toBuilder(): Builder {
        return builder().value(value).sourceLocation(sourceLocation)
    }

    class Builder : AbstractTraitBuilder<SpringDelegateTrait, Builder>() {
        var value: String? = null

        fun value(value: String): Builder {
            this.value = value
            return this
        }

        override fun build(): SpringDelegateTrait {
            return SpringDelegateTrait(this)
        }
    }

    companion object {
        val ID: ShapeId = ShapeId.from("com.example.smithy#springDelegate")

        fun builder(): Builder {
            return Builder()
        }
    }
}
