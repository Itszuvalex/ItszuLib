package com.itszuvalex.itszulib.api.adapters

import com.itszuvalex.itszulib.api.utility.MCConstants
import com.itszuvalex.itszulib.api.utility.Overideable
import com.itszuvalex.itszulib.api.wrappers.WrapperVanillaItemStack
import com.mojang.serialization.Codec
import net.minecraft.core.Direction
import net.minecraft.core.component.DataComponentPatch
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.Identifier
import net.minecraft.world.item.ItemStack

/**
 * Engine seam over [ItemStack], so storage logic can be unit tested without vanilla items.
 */
interface IItemStack : IModuleProvider {
    fun item(): Identifier

    fun stackSize(): Int

    fun setStackSize(size: Int)

    /**
     * THIS IS NOT VALIDATED. USE ONLY WHEN YOU KNOW `change` IS SAFE.
     *
     * @param change Amount to modify (+1, -1).
     */
    fun modifyStackSize(change: Int) = setStackSize(stackSize() + change)

    fun stackSizeMax(): Int

    fun damage(): Int

    fun setDamage(damage: Int)

    fun damageMax(): Int

    /**
     * @return Data components that differ from the item's defaults.
     */
    fun components(): DataComponentPatch

    fun hasComponents(): Boolean = !components().isEmpty

    fun toMinecraft(): ItemStack

    fun isEmpty(): Boolean

    fun room(): Int = stackSizeMax() - stackSize()

    fun copy(): IItemStack

    fun isItemEqual(other: IItemStack): Boolean

    companion object {
        @JvmField
        val Empty: IItemStack = object : IItemStack {
            override fun <T : Any> getModule(module: IModule<T>, side: Direction?): T? = null
            override fun item(): Identifier = BuiltInRegistries.ITEM.defaultKey
            override fun stackSize(): Int = 0
            override fun setStackSize(size: Int) {}
            override fun stackSizeMax(): Int = MCConstants.ITEMSTACK_MAX
            override fun damage(): Int = 0
            override fun setDamage(damage: Int) {}
            override fun damageMax(): Int = 0
            override fun components(): DataComponentPatch = DataComponentPatch.EMPTY
            override fun hasComponents(): Boolean = false
            override fun toMinecraft(): ItemStack = ItemStack.EMPTY
            override fun isEmpty(): Boolean = true
            override fun room(): Int = 0
            override fun copy(): IItemStack = this
            override fun isItemEqual(other: IItemStack): Boolean = other.isEmpty()
            override fun toString(): String = "IItemStack.Empty"
        }

        /**
         * Codec used to persist IItemStacks. Overridable so tests can serialize without vanilla ItemStacks.
         */
        @JvmField
        val CODEC: Overideable<Codec<IItemStack>> =
            Overideable(ItemStack.OPTIONAL_CODEC.xmap({ of(it) }, { it.toMinecraft() }))

        @JvmStatic
        fun codec(): Codec<IItemStack> = CODEC.get()

        @JvmStatic
        fun of(stack: ItemStack): IItemStack = if (stack.isEmpty) Empty else WrapperVanillaItemStack(stack)
    }
}
