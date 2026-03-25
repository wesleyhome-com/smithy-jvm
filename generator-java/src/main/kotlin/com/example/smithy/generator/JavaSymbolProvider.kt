package com.example.smithy.generator

import com.palantir.javapoet.ClassName
import software.amazon.smithy.codegen.core.Symbol
import software.amazon.smithy.codegen.core.SymbolProvider
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.shapes.*
import software.amazon.smithy.model.traits.EnumTrait
import software.amazon.smithy.model.traits.TagsTrait
import software.amazon.smithy.utils.StringUtils

class JavaSymbolProvider(
    private val model: Model, 
    private val basePackage: String,
    private val dtoSuffix: String = "DTO",
    private val serviceShape: ServiceShape? = null
) : SymbolProvider {

    override fun toSymbol(shape: Shape): Symbol {
        val builder = Symbol.builder().putProperty("shape", shape)
        
        if (shape is MemberShape) {
            val target = model.expectShape(shape.target)
            return toSymbol(target).toBuilder().putProperty("shape", shape).build()
        }

        val tagSuffix = getTagSuffix(shape)
        // Use service shape for safe renames if available
        val shapeName = if (serviceShape != null) shape.id.getName(serviceShape) else shape.id.name
        val capitalizedName = StringUtils.capitalize(shapeName)
        
        when (shape.type) {
            ShapeType.STRING -> {
                if (shape.hasTrait(EnumTrait::class.java)) {
                    val name = capitalizedName + dtoSuffix
                    builder.name(name).namespace("$basePackage.model$tagSuffix", ".")
                } else {
                    builder.name("String").namespace("java.lang", ".")
                }
            }
            ShapeType.INTEGER -> builder.name("Integer").namespace("java.lang", ".")
            ShapeType.LONG -> builder.name("Long").namespace("java.lang", ".")
            ShapeType.SHORT -> builder.name("Short").namespace("java.lang", ".")
            ShapeType.BOOLEAN -> builder.name("Boolean").namespace("java.lang", ".")
            ShapeType.FLOAT -> builder.name("Float").namespace("java.lang", ".")
            ShapeType.DOUBLE -> builder.name("Double").namespace("java.lang", ".")
            ShapeType.BIG_INTEGER -> builder.name("BigInteger").namespace("java.math", ".")
            ShapeType.BIG_DECIMAL -> builder.name("BigDecimal").namespace("java.math", ".")
            ShapeType.BYTE -> builder.name("Byte").namespace("java.lang", ".")
            ShapeType.BLOB -> builder.name("byte[]")
            ShapeType.DOCUMENT -> builder.name("String").namespace("java.lang", ".")
            ShapeType.TIMESTAMP -> builder.name("Instant").namespace("java.time", ".")
            ShapeType.LIST -> {
                val member = (shape as ListShape).member
                val memberSymbol = toSymbol(model.expectShape(member.target))
                builder.name("List").namespace("java.util", ".")
                       .putProperty("memberSymbol", memberSymbol)
            }
            ShapeType.MAP -> {
                val mapShape = shape as MapShape
                val keySymbol = toSymbol(model.expectShape(mapShape.key.target))
                val valueSymbol = toSymbol(model.expectShape(mapShape.value.target))
                builder.name("Map").namespace("java.util", ".")
                       .putProperty("keySymbol", keySymbol)
                       .putProperty("valueSymbol", valueSymbol)
            }
            ShapeType.STRUCTURE, ShapeType.UNION, ShapeType.ENUM -> {
                val name = if (shape.hasTrait(software.amazon.smithy.model.traits.ErrorTrait::class.java)) {
                    capitalizedName
                } else {
                    capitalizedName + dtoSuffix
                }
                builder.name(name).namespace("$basePackage.model$tagSuffix", ".")
            }
            ShapeType.INT_ENUM -> {
                val name = capitalizedName + dtoSuffix
                builder.name(name).namespace("$basePackage.model$tagSuffix", ".")
            }
            ShapeType.OPERATION -> {
                val name = capitalizedName + "Api"
                builder.name(name).namespace("$basePackage.api$tagSuffix", ".")
            }
            ShapeType.SERVICE -> {
                builder.name(capitalizedName).namespace(basePackage, ".")
            }
            else -> builder.name("Object").namespace("java.lang", ".")
        }
        
        return builder.build()
    }

    override fun toMemberName(member: MemberShape): String {
        return StringUtils.uncapitalize(member.memberName)
    }

    private fun getTagSuffix(shape: Shape): String {
        return shape.getTrait(TagsTrait::class.java)
            .map { tags -> 
                if (tags.values.isNotEmpty()) {
                    "." + tags.values[0].lowercase().replace(" ", "")
                } else ""
            }.orElse("")
    }
}
