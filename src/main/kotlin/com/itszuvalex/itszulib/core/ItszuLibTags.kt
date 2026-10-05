package com.itszuvalex.itszulib.core

import com.itszuvalex.itszulib.ItszuLib
import net.minecraft.core.registries.Registries
import net.minecraft.resources.Identifier
import net.minecraft.tags.TagKey
import net.minecraft.world.item.Item

/**
 * Tags ItszuLib reads. Add your mod's items to them in its own data (`data/itszulib/tags/item/<name>.json`).
 */
object ItszuLibTags {
    /**
     * Wrenches: a player sneaking and using one on a block entity block of [EntityBlockCore] breaks it at once, as if
     * mined, with its drops (kept contents included, and a multiblock drops as it does when broken).
     */
    @JvmField
    val WRENCHES: TagKey<Item> = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(ItszuLib.ID, "wrenches"))
}
