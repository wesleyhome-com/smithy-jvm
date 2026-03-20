package com.example.smithy.generator

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.isTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import software.amazon.smithy.build.MockManifest
import software.amazon.smithy.build.PluginContext
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.node.Node
import java.nio.file.Path

class SpringDelegateGeneratorPluginTest {

    @Test
    fun `plugin generates files correctly`(@TempDir tempDir: Path) {
        val model = Model.assembler()
            .addUnparsedModel("test.smithy", """
                namespace com.example
                service MyService {
                    version: "1.0",
                    operations: [SayHello]
                }
                @http(method: "POST", uri: "/hello")
                operation SayHello {
                    input: SayHelloInput,
                    output: SayHelloOutput
                }
                structure SayHelloInput {
                    @httpPayload
                    payload: HelloPayload
                }
                structure HelloPayload {
                    name: String
                }
                structure SayHelloOutput {
                    greeting: String
                }
            """.trimIndent())
            .assemble()
            .unwrap()
        
        val manifest = MockManifest(tempDir)
        val settings = Node.objectNodeBuilder()
            .withMember("package", "com.example.generated")
            .build()
            
        val context = PluginContext.builder()
            .model(model)
            .fileManifest(manifest)
            .settings(settings)
            .build()
            
        val plugin = SpringDelegateGeneratorPlugin()
        plugin.execute(context)
        
        // Check file locations
        assertThat(manifest.hasFile("com/example/generated/api/SayHelloApi.java")).isTrue()
        assertThat(manifest.hasFile("com/example/generated/controller/MyServiceController.java")).isTrue()
        assertThat(manifest.hasFile("com/example/generated/model/HelloPayloadDTO.java")).isTrue()
        
        val controllerCode = manifest.getFileString("com/example/generated/controller/MyServiceController.java").get()
        assertThat(controllerCode).contains("@RestController")
        assertThat(controllerCode).contains("SayHelloApi sayHelloApi")
    }

    @Test
    fun `plugin supports tag-based subpackages`(@TempDir tempDir: Path) {
        val model = Model.assembler()
            .addUnparsedModel("test.smithy", """
                namespace com.example
                service MyService {
                    version: "1.0",
                    operations: [AdminOp]
                }
                @http(method: "POST", uri: "/admin")
                @tags(["Admin"])
                operation AdminOp {
                    input: AdminInput
                }
                @tags(["Admin"])
                structure AdminInput {
                    @httpPayload
                    payload: String
                }
            """.trimIndent())
            .assemble()
            .unwrap()
        
        val manifest = MockManifest(tempDir)
        val settings = Node.objectNodeBuilder()
            .withMember("package", "com.example.generated")
            .build()
            
        val context = PluginContext.builder()
            .model(model)
            .fileManifest(manifest)
            .settings(settings)
            .build()
            
        val plugin = SpringDelegateGeneratorPlugin()
        plugin.execute(context)
        
        // Check tagged locations
        assertThat(manifest.hasFile("com/example/generated/api/admin/AdminOpApi.java")).isTrue()
        assertThat(manifest.hasFile("com/example/generated/model/admin/AdminInputDTO.java")).isTrue()
        assertThat(manifest.hasFile("com/example/generated/controller/AdminController.java")).isTrue()
    }
}
