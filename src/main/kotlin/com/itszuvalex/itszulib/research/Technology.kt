package com.itszuvalex.itszulib.research

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.ComponentSerialization
import net.minecraft.resources.Identifier
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import java.util.Optional

/**
 * One technology in a tech tree, loaded from datapacks (`data/<namespace>/itszulib/technology/<path>.json`, see
 * [TechTree.KEY]) and synced to clients. Which team has researched it, and how far, is team data
 * ([com.itszuvalex.itszulib.team.Research]); how research progress is produced is up to the mod.
 *
 * ```json
 * {
 *   "tree": "examplemod:main",
 *   "prerequisites": ["examplemod:basics"],
 *   "cost": 1000,
 *   "icon": "minecraft:redstone"
 * }
 * ```
 *
 * @param tree Which tree (screen) it belongs to; a mod usually has one. Prerequisites may be in other trees.
 * @param prerequisites Technologies that must all be researched before this one can be.
 * @param cost Research progress needed, in units the mod defines. 0: researched as soon as any progress is offered.
 * @param icon Item drawn for it.
 * @param name Defaults to the translation key `technology.<namespace>.<path>` (`/` in the path becomes `.`).
 * @param description Defaults to the name's key plus `.desc`.
 * @param position Where to draw it in its tree, in layout cells; unset to place it automatically ([TechTreeLayout]).
 * @param hidden Not shown until it can be researched.
 * @param unlockedByDefault Counts as researched for every team without being stored.
 */
data class Technology @JvmOverloads constructor(
    val tree: Identifier,
    val prerequisites: List<Identifier> = emptyList(),
    val cost: Long = 0L,
    val icon: Item = Items.BOOK,
    val name: Optional<Component> = Optional.empty(),
    val description: Optional<Component> = Optional.empty(),
    val position: Optional<TechTreeLayout.Point> = Optional.empty(),
    val hidden: Boolean = false,
    val unlockedByDefault: Boolean = false,
) {
    init {
        require(cost >= 0L) { "Technology cost must not be negative: $cost" }
    }

    fun iconStack(): ItemStack = ItemStack(icon)

    fun displayName(id: Identifier): Component = name.orElseGet { Component.translatable(translationKey(id)) }

    fun displayDescription(id: Identifier): Component = description.orElseGet { Component.translatable(translationKey(id) + ".desc") }

    companion object {
        @JvmStatic
        fun translationKey(id: Identifier): String = "technology.${id.namespace}.${id.path.replace('/', '.')}"

        private val POINT_CODEC: Codec<TechTreeLayout.Point> = RecordCodecBuilder.create { i ->
            i.group(
                Codec.FLOAT.fieldOf("x").forGetter(TechTreeLayout.Point::x),
                Codec.FLOAT.fieldOf("y").forGetter(TechTreeLayout.Point::y),
            ).apply(i, TechTreeLayout::Point)
        }

        @JvmField
        val CODEC: Codec<Technology> = RecordCodecBuilder.create { i ->
            i.group(
                Identifier.CODEC.fieldOf("tree").forGetter(Technology::tree),
                Identifier.CODEC.listOf().optionalFieldOf("prerequisites", emptyList()).forGetter(Technology::prerequisites),
                Codec.LONG.validate { if (it >= 0L) com.mojang.serialization.DataResult.success(it) else com.mojang.serialization.DataResult.error { "cost must not be negative: $it" } }
                    .optionalFieldOf("cost", 0L).forGetter(Technology::cost),
                BuiltInRegistries.ITEM.byNameCodec().optionalFieldOf("icon", Items.BOOK).forGetter(Technology::icon),
                ComponentSerialization.CODEC.optionalFieldOf("name").forGetter(Technology::name),
                ComponentSerialization.CODEC.optionalFieldOf("description").forGetter(Technology::description),
                POINT_CODEC.optionalFieldOf("position").forGetter(Technology::position),
                Codec.BOOL.optionalFieldOf("hidden", false).forGetter(Technology::hidden),
                Codec.BOOL.optionalFieldOf("unlocked_by_default", false).forGetter(Technology::unlockedByDefault),
            ).apply(i, ::Technology)
        }
    }
}

/**
 * Where a technology stands for one team.
 */
enum class TechnologyState {
    /** Researched, or unlocked by default. */
    RESEARCHED,

    /** Every prerequisite is researched: progress can be added. */
    AVAILABLE,

    /** Shown, but some prerequisite is not researched yet. */
    LOCKED,

    /** Not shown: hidden until available. */
    HIDDEN,
}
