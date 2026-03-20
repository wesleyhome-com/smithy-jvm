package com.example.smithy.generator

import assertk.assertThat
import assertk.assertions.contains
import org.junit.jupiter.api.Test
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.shapes.ShapeId
import software.amazon.smithy.model.shapes.StructureShape

class StructureGeneratorTest {

    @Test
    fun `generates java record for structure`() {
        val model = Model.assembler()
            .addUnparsedModel("test.smithy", """
                namespace com.example
                structure MyStructure {
                    foo: String,
                    bar: Integer
                }
            """.trimIndent())
            .assemble()
            .unwrap()
        
        val shapeId = ShapeId.from("com.example#MyStructure")
        val shape = model.expectShape(shapeId, StructureShape::class.java)
        val symbolProvider = JavaSymbolProvider(model, "com.example.generated")
        
        val generator = StructureGenerator(model, symbolProvider, shape)
        val javaFile = generator.generate()
        val code = javaFile.toString()
        
        assertThat(code).contains("record MyStructureDTO")
    }

    @Test
    fun `generates java record with collections`() {
        val model = Model.assembler()
            .addUnparsedModel("test.smithy", """
                namespace com.example
                structure ComplexStructure {
                    tags: StringList,
                    metadata: StringMap
                }
                list StringList {
                    member: String
                }
                map StringMap {
                    key: String,
                    value: String
                }
            """.trimIndent())
            .assemble()
            .unwrap()
        
        val shapeId = ShapeId.from("com.example#ComplexStructure")
        val shape = model.expectShape(shapeId, StructureShape::class.java)
        val symbolProvider = JavaSymbolProvider(model, "com.example.generated")
        
        val generator = StructureGenerator(model, symbolProvider, shape)
        val javaFile = generator.generate()
        val code = javaFile.toString()
        
        assertThat(code).contains("record ComplexStructureDTO")
        assertThat(code).contains("List<String> tags")
    }

    @Test
    fun `generates java record with validation annotations`() {
        val model = Model.assembler()
            .addUnparsedModel("test.smithy", """
                namespace com.example
                structure ValidatedStructure {
                    @required
                    name: String,
                    
                    @range(min: 10, max: 20)
                    age: Integer,
                    
                    @range(min: 5)
                    count: Integer,
                    
                    nested: NestedStructure
                }
                structure NestedStructure {
                    foo: String
                }
            """.trimIndent())
            .assemble()
            .unwrap()
        
        val shapeId = ShapeId.from("com.example#ValidatedStructure")
        val shape = model.expectShape(shapeId, StructureShape::class.java)
        val symbolProvider = JavaSymbolProvider(model, "com.example.generated")
        
        val generator = StructureGenerator(model, symbolProvider, shape)
        val javaFile = generator.generate()
        val code = javaFile.toString()
        
        assertThat(code).contains("record ValidatedStructureDTO")
        assertThat(code).contains("@NotNull @JsonProperty(\"name\") String name")
        assertThat(code).contains("@Range(min = 10, max = 20) @JsonProperty(\"age\") Integer age")
        assertThat(code).contains("@Min(5) @JsonProperty(\"count\") Integer count")
        assertThat(code).contains("NestedStructureDTO nested")
    }

    @Test
    fun `generates java record with javadoc documentation`() {
        val model = Model.assembler()
            .addUnparsedModel("test.smithy", """
                namespace com.example
                
                @documentation("This is a documented structure.")
                structure DocumentedStructure {
                    @documentation("This is a documented parameter.")
                    name: String
                }
            """.trimIndent())
            .assemble()
            .unwrap()
        
        val shapeId = ShapeId.from("com.example#DocumentedStructure")
        val shape = model.expectShape(shapeId, StructureShape::class.java)
        val symbolProvider = JavaSymbolProvider(model, "com.example.generated")
        
        val generator = StructureGenerator(model, symbolProvider, shape)
        val javaFile = generator.generate()
        val code = javaFile.toString()
        
        assertThat(code).contains("This is a documented structure.")
        assertThat(code).contains("@param name This is a documented parameter.")
    }

    @Test
    fun `generates java record with default values`() {
        val model = Model.assembler()
            .addUnparsedModel("test.smithy", """
                ${'$'}version: "2.0"
                namespace com.example
                
                structure DefaultStructure {
                    @default(0)
                    count: Integer
                    
                    @default(false)
                    active: Boolean
                    
                    @default("unknown")
                    status: String
                }
            """.trimIndent())
            .assemble()
            .unwrap()
        
        val shapeId = ShapeId.from("com.example#DefaultStructure")
        val shape = model.expectShape(shapeId, StructureShape::class.java)
        val symbolProvider = JavaSymbolProvider(model, "com.example.generated")
        
        val generator = StructureGenerator(model, symbolProvider, shape)
        val javaFile = generator.generate()
        val code = javaFile.toString()
        
        assertThat(code).contains("if (count == null) count = 0;")
        assertThat(code).contains("if (active == null) active = false;")
        assertThat(code).contains("if (status == null) status = \"unknown\";")
    }
}
