package com.example.smithy.generator

import com.palantir.javapoet.AnnotationSpec
import com.palantir.javapoet.ClassName
import com.palantir.javapoet.JavaFile
import com.palantir.javapoet.MethodSpec
import com.palantir.javapoet.TypeSpec
import java.util.logging.Logger
import javax.lang.model.element.Modifier
import software.amazon.smithy.build.PluginContext
import software.amazon.smithy.build.SmithyBuildPlugin
import software.amazon.smithy.codegen.core.SymbolProvider
import software.amazon.smithy.model.shapes.EnumShape
import software.amazon.smithy.model.shapes.OperationShape
import software.amazon.smithy.model.shapes.ServiceShape
import software.amazon.smithy.model.shapes.Shape
import software.amazon.smithy.model.shapes.StringShape
import software.amazon.smithy.model.shapes.StructureShape
import software.amazon.smithy.model.shapes.UnionShape
import software.amazon.smithy.model.traits.EnumTrait
import software.amazon.smithy.model.traits.ErrorTrait
import software.amazon.smithy.model.traits.TagsTrait
import software.amazon.smithy.model.validation.ValidatedResultException
import software.amazon.smithy.model.validation.ValidationEvent
import software.amazon.smithy.utils.StringUtils

class SpringDelegateGeneratorPlugin : SmithyBuildPlugin {
    private val LOGGER = Logger.getLogger(SpringDelegateGeneratorPlugin::class.java.name)

    override fun getName(): String {
        return "spring-delegate-generator"
    }

    override fun execute(context: PluginContext) {
        LOGGER.info("Executing Spring Delegate Generator Plugin")
        val model = context.model
        val settings = context.settings
        val manifest = context.fileManifest

        val validationEvents = mutableListOf<ValidationEvent>()

        val basePackage = settings.getStringMember("package").map { it.value }.orElse("com.example.generated")
        val useResponseEntity = settings.getBooleanMemberOrDefault("useResponseEntity", true)
        val dtoSuffix = settings.getStringMember("dtoSuffix").map { it.value }.orElse("DTO")
        val symbolProvider = JavaSymbolProvider(model, basePackage, dtoSuffix)

        val allStructures = model.shapes(StructureShape::class.java).toList()
            .filter { !it.id.namespace.equals("smithy.api") }

        val errorShapes = allStructures.filter { it.hasTrait(ErrorTrait::class.java) }
        val pojoShapes = allStructures.filter { !it.hasTrait(ErrorTrait::class.java) }

        // Generate POJOs for all structures (skipping errors and smithy internals)
        val structureFiles = pojoShapes.map { structure ->
            StructureGenerator(model, symbolProvider, structure).generate()
        } + errorShapes.map { structure ->
            // Generate Exceptions for error shapes
            ExceptionGenerator(model, symbolProvider, structure).generate()
        }

        // Generate Unions
        val unionFiles = model.shapes(UnionShape::class.java).toList().filter { !it.id.namespace.equals("smithy.api") }
            .map { unionShape ->
                UnionGenerator(model, symbolProvider, unionShape).generate()
            }

        // Generate Enums (Smithy 1.0 @enum on StringShape and Smithy 2.0 EnumShape)
        val stringEnumFiles = model.shapes(StringShape::class.java).toList()
            .filter { !it.id.namespace.equals("smithy.api") && it.hasTrait(EnumTrait::class.java) }.map { stringShape ->
                EnumGenerator(model, symbolProvider, stringShape).generate()
            }

        val enumFiles = model.shapes(EnumShape::class.java).toList().filter { !it.id.namespace.equals("smithy.api") }
            .map { enumShape ->
                EnumGenerator(model, symbolProvider, enumShape).generate()
            }

        // Generate Global Exception Handler if errors exist
        val exceptionHandlerFiles = if (errorShapes.isNotEmpty()) {
            val handlerGenerator = GlobalExceptionHandlerGenerator(model, symbolProvider, basePackage)
            listOf(handlerGenerator.generate(errorShapes))
        } else emptyList()

        // Generate Operations, Controllers and Fallback Config
        val serviceFiles =
            model.shapes(ServiceShape::class.java).toList().filter { !it.id.namespace.equals("smithy.api") }
                .flatMap { service ->
                    val serviceSymbol = symbolProvider.toSymbol(service)
                    val operations = service.operations.map { model.expectShape(it, OperationShape::class.java) }

                    // 1. Generate individual Operation APIs and Stubs
                    val apiFiles = operations.flatMap { operation ->
                        OperationApiGenerator(model, symbolProvider, operation, useResponseEntity).generate()
                    }

                    // 2. Generate Fallback Configuration
                    val configClassName = "SpringDelegateFallbackConfiguration"
                    val configPackage = "$basePackage.config"
                    val fallbackFile =
                        generateFallbackConfig(operations, symbolProvider, configClassName, configPackage)

                    // 3. Group Operations by Tag for Controllers
                    val groupedOperations = operations.groupBy { getPrimaryTag(it) }

                    val controllerResults = groupedOperations.map { (tag, ops) ->
                        val controllerName = if (tag != null) {
                            "${StringUtils.capitalize(tag)}Controller"
                        } else {
                            "${serviceSymbol.name}Controller"
                        }
                        val controllerPackage = "$basePackage.controller"

                        ControllerGenerator(
                            model, symbolProvider, controllerName, controllerPackage, ops, useResponseEntity
                        ).generate()
                    }

                    val controllerFiles = controllerResults.map { it.first }
                    validationEvents.addAll(controllerResults.flatMap { it.second })

                    apiFiles + fallbackFile + controllerFiles
                }

        val allJavaFiles =
            structureFiles + unionFiles + stringEnumFiles + enumFiles + exceptionHandlerFiles + serviceFiles

        // --- COMMIT PHASE ---
        if (validationEvents.isNotEmpty()) {
            throw ValidatedResultException(validationEvents)
        }

        // Only write files if there were no validation errors
        for (javaFile in allJavaFiles) {
            manifest.writeFile(javaFile.toPath(), javaFile.toString())
        }

        // Write AutoConfiguration.imports if successful
        val configClassName = "SpringDelegateFallbackConfiguration"
        val configPackage = "$basePackage.config"
        val importsPath = "META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports"
        val fullClassName = "$configPackage.$configClassName"
        manifest.writeFile(importsPath, fullClassName + "\n")
    }

    private fun generateFallbackConfig(operations: List<OperationShape>,
                                       symbolProvider: SymbolProvider,
                                       className: String,
                                       packageName: String
    ): JavaFile {
        val typeBuilder = TypeSpec.classBuilder(className).addModifiers(Modifier.PUBLIC)
            .addAnnotation(ClassName.get("org.springframework.boot.autoconfigure", "AutoConfiguration")).addJavadoc(
                "Fallback configuration that provides default stub implementations for Smithy defined operations.\n" + "<p>\n" + "These stubs will be used by the generated controllers if no custom implementation\n" + "is provided as a Spring Bean in the application context.\n"
            )

        for (operation in operations) {
            val opSymbol = symbolProvider.toSymbol(operation)
            val interfaceName = opSymbol.name
            val interfacePackage = opSymbol.namespace
            val stubPackage = "$interfacePackage.stub"
            val stubName = "${interfaceName}Stub"

            val interfaceType = ClassName.get(interfacePackage, interfaceName)
            val stubType = ClassName.get(stubPackage, stubName)

            val beanMethod = MethodSpec.methodBuilder(StringUtils.uncapitalize(stubName)).addModifiers(Modifier.PUBLIC)
                .addAnnotation(ClassName.get("org.springframework.context.annotation", "Bean")).addAnnotation(
                    AnnotationSpec.builder(
                        ClassName.get(
                            "org.springframework.boot.autoconfigure.condition", "ConditionalOnMissingBean"
                        )
                    ).addMember("value", "\$T.class", interfaceType).build()
                ).returns(interfaceType).addStatement("return new \$T()", stubType).build()

            typeBuilder.addMethod(beanMethod)
        }

        return JavaFile.builder(packageName, typeBuilder.build()).build()
    }

    private fun getPrimaryTag(shape: Shape): String? {
        return shape.getTrait(TagsTrait::class.java).map { it.values.firstOrNull() }.orElse(null)
    }

    private fun JavaFile.toPath(): String {
        return "${this.packageName().replace(".", "/")}/${this.typeSpec().name()}.java"
    }
}
