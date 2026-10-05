package com.itszuvalex.itszulib.core

import net.minecraft.network.chat.Component

/**
 * What breaking a block does with the contents a fragment holds.
 *
 * - [DROP]: the contents spill into the world as item entities, like a vanilla chest. Items only.
 * - [KEEP]: the contents stay with the block: they are written to the dropped block item
 *   (`itszulib:fragment_data`, [com.itszuvalex.itszulib.api.Components.FRAGMENT_DATA]) and restored when it is placed
 *   again, like a vanilla shulker box. Works for any contents a fragment can save. Only a block that is broken so that
 *   it drops (by a player in survival, an explosion, `destroyBlock` with drops) keeps them; replacing or `/setblock`ing
 *   it loses them, as with a shulker box. An empty block drops a plain item, so empty and fresh ones stack.
 * - [DISCARD]: the contents are lost. For views of storage owned elsewhere, or contents that are meant to vanish.
 */
enum class BreakBehavior {
    DROP,
    KEEP,
    DISCARD,
}

/**
 * A fragment that holds contents and says what happens to them when the block is broken ([BreakBehavior]). The block
 * entity asks its fragments ([BlockEntityCore.keepsContents]) to decide whether the dropped item carries data.
 */
interface IBreakContents {
    val breakBehavior: BreakBehavior

    /** Whether there is nothing to keep or drop. */
    fun isContentEmpty(): Boolean

    /**
     * Lines saying how much is used of how much it can hold (for an item's tooltip, see [ContentBlockItem]), such as
     * "Items: 3 / 54 slots". Empty for contents with nothing to say.
     */
    fun describe(): List<Component> = emptyList()
}
