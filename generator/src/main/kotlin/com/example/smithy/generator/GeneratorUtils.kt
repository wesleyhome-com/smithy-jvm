package com.example.smithy.generator

import com.palantir.javapoet.*
import software.amazon.smithy.codegen.core.Symbol
import software.amazon.smithy.model.shapes.MemberShape
import software.amazon.smithy.model.shapes.StructureShape
import software.amazon.smithy.model.traits.HttpHeaderTrait
import software.amazon.smithy.model.traits.HttpLabelTrait
import software.amazon.smithy.model.traits.HttpQueryTrait

fun StructureShape.getMetadataAndPayload(): Pair<List<MemberShape>, List<MemberShape>> {
    val metadataMembers = this.allMembers.values.filter { 
        it.hasTrait(HttpLabelTrait::class.java) ||
        it.hasTrait(HttpQueryTrait::class.java) ||
        it.hasTrait(HttpHeaderTrait::class.java)
    }
    val payloadMembers = this.allMembers.values.filter { !metadataMembers.contains(it) }
    return Pair(metadataMembers, payloadMembers)
}

fun Symbol.toTypeName(): TypeName {
    if (this.name == "List") {
        val memberSymbol = this.getProperty("memberSymbol", Symbol::class.java).get()
        return ParameterizedTypeName.get(ClassName.get("java.util", "List"), memberSymbol.toTypeName())
    }
    if (this.name == "Map") {
        val keySymbol = this.getProperty("keySymbol", Symbol::class.java).get()
        val valueSymbol = this.getProperty("valueSymbol", Symbol::class.java).get()
        return ParameterizedTypeName.get(ClassName.get("java.util", "Map"), keySymbol.toTypeName(), valueSymbol.toTypeName())
    }
    return getBaseTypeName(this.name, this.namespace)
}

private fun getBaseTypeName(name: String, namespace: String = ""): TypeName {
    return when (name) {
        "String" -> ClassName.get("java.lang", "String")
        "Integer" -> ClassName.get("java.lang", "Integer")
        "Long" -> ClassName.get("java.lang", "Long")
        "Boolean" -> ClassName.get("java.lang", "Boolean")
        "Float" -> ClassName.get("java.lang", "Float")
        "Double" -> ClassName.get("java.lang", "Double")
        "Byte" -> ClassName.get("java.lang", "Byte")
        "byte[]" -> ArrayTypeName.of(TypeName.BYTE)
        "OffsetDateTime" -> ClassName.get("java.time", "OffsetDateTime")
        else -> if (namespace.isNotEmpty()) ClassName.get(namespace, name) else ClassName.bestGuess(name)
    }
}
