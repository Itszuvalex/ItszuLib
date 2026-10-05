package com.itszuvalex.itszulib.verify

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.Identifier
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.EntityBlock
import net.minecraft.world.item.Item

/**
 * Consistency checks over everything a mod registers and the resources that describe it, for the mod's own game tests
 * (`assertValueEqual(ContentIntegrity.problems("mymod", MyMod::class.java.classLoader), emptyList(), "problems")`).
 * Each check returns a list of readable problems, empty when all is well, so a failing test names every missing file
 * at once instead of the first.
 *
 * Resources are read through a [ClassLoader] (pass the mod's own), so generated resources are seen in dev runs and in
 * the jar alike.
 *
 * Non-model resources named by `model` (OBJ files) are paths under `assets/<namespace>/`. Checked for each block and item in the mod's namespace: the blockstate or item definition exists; every model it
 * names, and each model's parents, exist; every texture those models name exists; a translation exists for the
 * description id; blocks that drop something have a loot table file. Block entity types must have valid blocks, and
 * every [EntityBlock] must be valid for some type. Only references into the mod's own namespace are followed;
 * vanilla and other mods' resources are not looked up.
 */
object ContentIntegrity {
    /**
     * Everything wrong with [namespace]'s registered content: [assetProblems] and [blockEntityProblems].
     */
    @JvmStatic
    @JvmOverloads
    fun problems(namespace: String, loader: ClassLoader = ContentIntegrity::class.java.classLoader, language: String = "en_us"): List<String> =
        assetProblems(namespace, loader, language) + blockEntityProblems(namespace)

    /**
     * Missing blockstates, item definitions, models, textures, translations and loot tables for [namespace]'s
     * registered blocks and items.
     */
    @JvmStatic
    @JvmOverloads
    fun assetProblems(namespace: String, loader: ClassLoader = ContentIntegrity::class.java.classLoader, language: String = "en_us"): List<String> {
        val problems = ArrayList<String>()
        val resources = Resources(loader)
        val lang = resources.json("assets/$namespace/lang/$language.json") as? JsonObject
        if (lang == null) problems += "Missing language file assets/$namespace/lang/$language.json"
        val models = LinkedHashMap<String, String>() // model id -> what needs it

        for ((id, block) in blocks(namespace)) {
            val owner = "block $id"
            val state = resources.json("assets/$namespace/blockstates/${id.path}.json")
            if (state == null) problems += "$owner has no blockstate assets/$namespace/blockstates/${id.path}.json"
            else referencedModels(state).forEach { models.putIfAbsent(it, owner) }
            if (block.lootTable.isPresent && !resources.exists("data/$namespace/loot_table/blocks/${id.path}.json")) {
                problems += "$owner has no loot table data/$namespace/loot_table/blocks/${id.path}.json"
            }
            if (lang != null && !lang.has(block.descriptionId)) problems += "$owner has no translation ${block.descriptionId}"
        }
        for ((id, item) in items(namespace)) {
            val owner = "item $id"
            val definition = resources.json("assets/$namespace/items/${id.path}.json")
            if (definition == null) problems += "$owner has no item definition assets/$namespace/items/${id.path}.json"
            else referencedModels(definition).forEach { models.putIfAbsent(it, owner) }
            if (lang != null && !lang.has(item.descriptionId)) problems += "$owner has no translation ${item.descriptionId}"
        }

        val seen = HashSet<String>()
        val queue = ArrayDeque(models.entries.map { it.key to it.value })
        while (queue.isNotEmpty()) {
            val (model, owner) = queue.removeFirst()
            if (!seen.add(model)) continue
            val (ns, path) = split(model)
            if (ns != namespace) continue
            val isJson = !path.substringAfterLast('/').contains('.')
            val file = if (isJson) "assets/$ns/models/$path.json" else "assets/$ns/$path"
            if (!isJson) {
                if (!resources.exists(file)) problems += "$owner needs $model, but $file is missing"
                continue
            }
            val json = resources.json(file)
            if (json == null) {
                problems += "$owner needs model $model, but $file is missing"
                continue
            }
            for (texture in referencedTextures(json)) {
                val (tns, tpath) = split(texture)
                if (tns == namespace && !resources.exists("assets/$tns/textures/$tpath.png")) {
                    problems += "model $model needs texture $texture, but assets/$tns/textures/$tpath.png is missing"
                }
            }
            referencedModels(json).forEach { queue += it to "model $model" }
        }
        return problems.distinct()
    }

    /**
     * Block entity types of [namespace] with no valid block, and blocks that create block entities
     * ([EntityBlock]) but belong to no block entity type (they would crash when placed).
     */
    @JvmStatic
    fun blockEntityProblems(namespace: String): List<String> {
        val problems = ArrayList<String>()
        for ((key, type) in BuiltInRegistries.BLOCK_ENTITY_TYPE.entrySet()) {
            if (key.identifier().namespace == namespace && type.validBlocks.isEmpty()) {
                problems += "Block entity type ${key.identifier()} has no valid blocks"
            }
        }
        for ((id, block) in blocks(namespace)) {
            if (block !is EntityBlock) continue
            val state = block.defaultBlockState()
            if (BuiltInRegistries.BLOCK_ENTITY_TYPE.none { it.isValid(state) }) {
                problems += "Block $id is an EntityBlock but valid for no block entity type"
            }
        }
        return problems
    }

    /**
     * Every model id a blockstate, item definition or model names: string values under `model` and `parent`, found
     * anywhere in the document. Pure, for testing without a game.
     */
    @JvmStatic
    fun referencedModels(json: JsonElement): Set<String> {
        val found = LinkedHashSet<String>()
        fun walk(e: JsonElement) {
            when {
                e.isJsonObject -> for ((k, v) in e.asJsonObject.entrySet()) {
                    if ((k == "model" || k == "parent") && v.isJsonPrimitive && v.asJsonPrimitive.isString) found += v.asString else walk(v)
                }
                e.isJsonArray -> e.asJsonArray.forEach(::walk)
            }
        }
        walk(json)
        return found
    }

    /**
     * The texture ids a model names in its `textures` map, leaving out references to other slots (`#name`).
     */
    @JvmStatic
    fun referencedTextures(json: JsonElement): Set<String> {
        val textures = (json as? JsonObject)?.get("textures") as? JsonObject ?: return emptySet()
        return textures.entrySet()
            .mapNotNull { (_, v) -> if (v.isJsonPrimitive) v.asString else null }
            .filterTo(LinkedHashSet()) { !it.startsWith("#") }
    }

    private fun blocks(namespace: String): List<Pair<Identifier, Block>> =
        BuiltInRegistries.BLOCK.entrySet().map { it.key.identifier() to it.value }.filter { it.first.namespace == namespace }.sortedBy { it.first.path }

    private fun items(namespace: String): List<Pair<Identifier, Item>> =
        BuiltInRegistries.ITEM.entrySet().map { it.key.identifier() to it.value }.filter { it.first.namespace == namespace }.sortedBy { it.first.path }

    private fun split(id: String): Pair<String, String> =
        if (':' in id) id.substringBefore(':') to id.substringAfter(':') else "minecraft" to id

    private class Resources(private val loader: ClassLoader) {
        private val cache = HashMap<String, JsonElement?>()

        fun exists(path: String): Boolean = loader.getResource(path) != null

        fun json(path: String): JsonElement? = cache.getOrPut(path) {
            loader.getResourceAsStream(path)?.use { JsonParser.parseReader(it.reader()) }
        }
    }
}
