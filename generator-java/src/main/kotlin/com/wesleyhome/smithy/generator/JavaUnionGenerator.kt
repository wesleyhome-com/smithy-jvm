package com.wesleyhome.smithy.generator

import com.palantir.javapoet.ClassName
import com.palantir.javapoet.JavaFile
import com.palantir.javapoet.MethodSpec
import com.palantir.javapoet.TypeSpec
import software.amazon.smithy.codegen.core.SymbolProvider
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.shapes.UnionShape
import software.amazon.smithy.utils.StringUtils
import javax.lang.model.element.Modifier

/**
 * Generates a Java sealed interface for a Smithy UnionShape.
 */
class JavaUnionGenerator(
    private val codegenContext: JavaCodegenContext? = null
) : ShapeGenerator<UnionShape> {
    override val shapeType: Class<UnionShape> = UnionShape::class.java

    override fun generate(shape: UnionShape, model: Model, symbolProvider: SymbolProvider): ShapeGenerator.Result {
        val symbol = symbolProvider.toSymbol(shape)
        val className = ClassName.get(symbol.namespace, symbol.name)

        val unknownName = "Unknown"
        val unknownClassName = className.nestedClass(unknownName)

        val typeBuilder = TypeSpec.interfaceBuilder(className)
            .addModifiers(Modifier.PUBLIC, Modifier.SEALED)

        if (shape.hasTrait(software.amazon.smithy.model.traits.DeprecatedTrait::class.java)) {
            typeBuilder.addAnnotation(Deprecated::class.java)
        }
        val variants = mutableListOf<JavaUnionVariant>()

        for (member in shape.allMembers.values) {
            val memberName = StringUtils.capitalize(member.memberName)
            val variantClassName = className.nestedClass(memberName)
            val memberSymbol = symbolProvider.toSymbol(member)
            val typeName = memberSymbol.toTypeName()
            variants.add(JavaUnionVariant(member, variantClassName))

            val recordBuilder = TypeSpec.recordBuilder(memberName)
                .addModifiers(Modifier.FINAL, Modifier.PUBLIC, Modifier.STATIC)
                .addSuperinterface(className)

            if (member.hasTrait(software.amazon.smithy.model.traits.DeprecatedTrait::class.java)) {
                recordBuilder.addAnnotation(Deprecated::class.java)
            }

            recordBuilder.recordConstructor(
                MethodSpec.constructorBuilder()
                    .addParameter(typeName, "value")
                    .build()
            )

            typeBuilder.addType(recordBuilder.build())
            typeBuilder.addPermittedSubclass(variantClassName)
        }

        // Add Unknown variant
        val unknownBuilder = TypeSpec.classBuilder(unknownName)
            .addModifiers(Modifier.PUBLIC, Modifier.FINAL, Modifier.STATIC)
            .addSuperinterface(className)

        typeBuilder.addType(unknownBuilder.build())
        typeBuilder.addPermittedSubclass(unknownClassName)
        codegenContext?.let { ctx ->
            ctx.integrations.forEach { integration ->
	            (integration as? JavaPoetCodegenIntegration)?.onUnionGenerated(
		            ctx,
		            shape,
		            typeBuilder,
		            unknownClassName,
		            variants
	            )
            }
        }
        codegenContext?.let { ctx ->
            ctx.integrations.forEach { integration ->
	            (integration as? JavaPoetCodegenIntegration)?.onShapeGenerated(ctx, shape, typeBuilder)
            }
        }

        val javaFile = JavaFile.builder(symbol.namespace, typeBuilder.build()).build()
        return ShapeGenerator.Result(listOf(javaFile.toGeneratedFile()))
    }
}
