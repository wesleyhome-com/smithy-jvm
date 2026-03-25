package com.wesleyhome.smithy.generator

import com.palantir.javapoet.*
import software.amazon.smithy.codegen.core.Symbol
import software.amazon.smithy.model.shapes.MemberShape
import software.amazon.smithy.model.shapes.StructureShape
import software.amazon.smithy.model.traits.HttpHeaderTrait
import software.amazon.smithy.model.traits.HttpLabelTrait
import software.amazon.smithy.model.traits.HttpQueryTrait

import software.amazon.smithy.codegen.core.MappedReservedWords
import software.amazon.smithy.codegen.core.ReservedWords

val JavaReservedWords: ReservedWords = MappedReservedWords.builder()
    .put("abstract", "abstract_")
    .put("assert", "assert_")
    .put("boolean", "boolean_")
    .put("break", "break_")
    .put("byte", "byte_")
    .put("case", "case_")
    .put("catch", "catch_")
    .put("char", "char_")
    .put("class", "class_")
    .put("const", "const_")
    .put("continue", "continue_")
    .put("default", "default_")
    .put("do", "do_")
    .put("double", "double_")
    .put("else", "else_")
    .put("enum", "enum_")
    .put("extends", "extends_")
    .put("final", "final_")
    .put("finally", "finally_")
    .put("float", "float_")
    .put("for", "for_")
    .put("goto", "goto_")
    .put("if", "if_")
    .put("implements", "implements_")
    .put("import", "import_")
    .put("instanceof", "instanceof_")
    .put("int", "int_")
    .put("interface", "interface_")
    .put("long", "long_")
    .put("native", "native_")
    .put("new", "new_")
    .put("package", "package_")
    .put("private", "private_")
    .put("protected", "protected_")
    .put("public", "public_")
    .put("return", "return_")
    .put("short", "short_")
    .put("static", "static_")
    .put("strictfp", "strictfp_")
    .put("super", "super_")
    .put("switch", "switch_")
    .put("synchronized", "synchronized_")
    .put("this", "this_")
    .put("throw", "throw_")
    .put("throws", "throws_")
    .put("transient", "transient_")
    .put("try", "try_")
    .put("void", "void_")
    .put("volatile", "volatile_")
    .put("while", "while_")
    .put("var", "var_")
    .put("record", "record_")
    .put("yield", "yield_")
    .put("sealed", "sealed_")
    .put("non-sealed", "non_sealed_")
    .put("permits", "permits_")
    .build()

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
        "BigInteger" -> ClassName.get("java.math", "BigInteger")
        "BigDecimal" -> ClassName.get("java.math", "BigDecimal")
        "byte[]" -> ArrayTypeName.of(TypeName.BYTE)
        "Instant" -> ClassName.get("java.time", "Instant")
        else -> if (namespace.isNotEmpty()) ClassName.get(namespace, name) else ClassName.bestGuess(name)
    }
}

fun JavaFile.toGeneratedFile(): GeneratedFile {
    val path = "${this.packageName().replace(".", "/")}/${this.typeSpec().name()}.java"
    return GeneratedFile(path = path, content = this.toString())
}
