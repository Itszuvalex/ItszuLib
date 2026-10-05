package com.itszuvalex.itszulib.verify

import com.google.gson.JsonParser
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ContentIntegrityTest {
    private fun json(text: String) = JsonParser.parseString(text)

    @Test
    fun ReferencedModels_Blockstate_FindsVariantsAndMultipart() {
        val state = json(
            """{"variants": {"facing=north": {"model": "m:block/a", "y": 90}, "facing=south": [{"model": "m:block/b"}]},
               "multipart": [{"when": {"up": "true"}, "apply": {"model": "m:block/c"}}]}""",
        )
        assertEquals(setOf("m:block/a", "m:block/b", "m:block/c"), ContentIntegrity.referencedModels(state))
    }

    @Test
    fun ReferencedModels_ItemDefinition_FindsNestedModels() {
        val definition = json(
            """{"model": {"type": "minecraft:condition", "on_true": {"type": "minecraft:model", "model": "m:item/x"},
               "on_false": {"type": "minecraft:model", "model": "m:item/y"}}}""",
        )
        assertEquals(setOf("m:item/x", "m:item/y"), ContentIntegrity.referencedModels(definition))
    }

    @Test
    fun ReferencedModels_ModelWithParent_FindsParentAndObjModel() {
        val model = json("""{"parent": "neoforge:item/default", "loader": "neoforge:obj", "model": "m:models/block/obj/x.obj"}""")
        assertEquals(setOf("neoforge:item/default", "m:models/block/obj/x.obj"), ContentIntegrity.referencedModels(model))
    }

    @Test
    fun ReferencedTextures_SkipsSlotReferences() {
        val model = json("""{"textures": {"all": "m:block/stone", "particle": "#all", "top": "block/dirt"}}""")
        assertEquals(setOf("m:block/stone", "block/dirt"), ContentIntegrity.referencedTextures(model))
    }

    @Test
    fun ReferencedTextures_NoTextures_Empty() {
        assertEquals(emptySet<String>(), ContentIntegrity.referencedTextures(json("""{"parent": "m:block/a"}""")))
    }
}
