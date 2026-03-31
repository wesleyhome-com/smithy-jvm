package com.wesleyhome.smithy.generator

import com.palantir.javapoet.AnnotationSpec
import com.palantir.javapoet.ClassName
import com.palantir.javapoet.JavaFile
import com.palantir.javapoet.MethodSpec
import com.palantir.javapoet.TypeSpec
import software.amazon.smithy.codegen.core.SymbolProvider
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.knowledge.TopDownIndex
import software.amazon.smithy.model.shapes.ServiceShape
import software.amazon.smithy.utils.StringUtils
import javax.lang.model.element.Modifier

/**
 * Generates Spring Boot AutoConfiguration for fallback stubs of Smithy operations.
 */
class JavaSpringFallbackConfigGenerator : ShapeGenerator<ServiceShape> {
	override val shapeType: Class<ServiceShape> = ServiceShape::class.java

	override fun generate(shape: ServiceShape, model: Model, symbolProvider: SymbolProvider): ShapeGenerator.Result {
		val serviceSymbol = symbolProvider.toSymbol(shape)
		val topDownIndex = TopDownIndex.of(model)
		val operations = topDownIndex.getContainedOperations(shape).toList()

		val className = "SpringDelegateFallbackConfiguration"
		// Base package or a config subpackage
		val packageName = "${serviceSymbol.namespace}.config"

		val typeBuilder = TypeSpec.classBuilder(className)
			.addModifiers(Modifier.PUBLIC)
			.addAnnotation(ClassName.get("org.springframework.boot.autoconfigure", "AutoConfiguration"))
			.addJavadoc(
				"Fallback configuration that provides default stub implementations for Smithy defined operations.\n" +
					"<p>\n" +
					"These stubs will be used by the generated controllers if no custom implementation\n" +
					"is provided as a Spring Bean in the application context.\n"
			)

		for (operation in operations) {
			val opSymbol = symbolProvider.toSymbol(operation)
			val interfaceName = opSymbol.name
			val interfacePackage = opSymbol.namespace
			val stubPackage = "$interfacePackage.stub"
			val stubName = "${interfaceName}Stub"

			val interfaceType = ClassName.get(interfacePackage, interfaceName)
			val stubType = ClassName.get(stubPackage, stubName)

			val beanMethod = MethodSpec.methodBuilder(StringUtils.uncapitalize(stubName))
				.addModifiers(Modifier.PUBLIC)
				.addAnnotation(ClassName.get("org.springframework.context.annotation", "Bean"))
				.addAnnotation(
					AnnotationSpec.builder(
						ClassName.get("org.springframework.boot.autoconfigure.condition", "ConditionalOnMissingBean")
					).addMember("value", $$"$T.class", interfaceType).build()
				)
				.returns(interfaceType)
				.addStatement($$"return new $T()", stubType)
				.build()

			typeBuilder.addMethod(beanMethod)
		}

		val javaFile = JavaFile.builder(packageName, typeBuilder.build()).build()
		return ShapeGenerator.Result(listOf(javaFile.toGeneratedFile()))
	}
}
