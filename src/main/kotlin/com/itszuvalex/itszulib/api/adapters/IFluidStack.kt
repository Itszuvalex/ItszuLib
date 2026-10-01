package com.itszuvalex.itszulib.api.adapters

import com.itszuvalex.itszulib.api.utility.Overideable
import com.itszuvalex.itszulib.api.wrappers.WrapperVanillaFluidStack
import com.mojang.serialization.Codec
import net.minecraft.core.component.DataComponentPatch
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.Identifier
import net.neoforged.neoforge.fluids.FluidStack
import net.neoforged.neoforge.transfer.fluid.FluidResource

/**
 * Engine seam over [FluidStack], so fluid storage logic can be unit tested without vanilla fluids. Port of ItszuLib
 * 1.12.2's `IFluidStack`. Amounts are in millibuckets.
 */
interface IFluidStack {
    fun fluid(): Identifier

    fun amount(): Int

    fun setAmount(amount: Int)

    /**
     * @return Data components that differ from the fluid's defaults.
     */
    fun components(): DataComponentPatch

    fun toMinecraft(): FluidStack

    fun isEmpty(): Boolean

    fun copy(): IFluidStack

    fun copyWithAmount(amount: Int): IFluidStack = if (amount <= 0) Empty else copy().also { it.setAmount(amount) }

    /**
     * @return True if both hold the same fluid with the same components (amounts may differ), or both are empty.
     */
    fun isFluidEqual(other: IFluidStack): Boolean

    companion object {
        @JvmField
        val Empty: IFluidStack = object : IFluidStack {
            override fun fluid(): Identifier = BuiltInRegistries.FLUID.defaultKey
            override fun amount(): Int = 0
            override fun setAmount(amount: Int) {}
            override fun components(): DataComponentPatch = DataComponentPatch.EMPTY
            override fun toMinecraft(): FluidStack = FluidStack.EMPTY
            override fun isEmpty(): Boolean = true
            override fun copy(): IFluidStack = this
            override fun isFluidEqual(other: IFluidStack): Boolean = other.isEmpty()
            override fun toString(): String = "IFluidStack.Empty"
        }

        /**
         * Codec used to persist IFluidStacks. Overridable so tests can serialize without vanilla fluids.
         */
        @JvmField
        val CODEC: Overideable<Codec<IFluidStack>> =
            Overideable(FluidStack.OPTIONAL_CODEC.xmap({ of(it) }, { it.toMinecraft() }))

        @JvmStatic
        fun codec(): Codec<IFluidStack> = CODEC.get()

        @JvmStatic
        fun of(stack: FluidStack): IFluidStack = if (stack.isEmpty) Empty else WrapperVanillaFluidStack(stack)

        @JvmStatic
        fun of(resource: FluidResource, amount: Int): IFluidStack =
            if (resource.isEmpty || amount <= 0) Empty else WrapperVanillaFluidStack(resource.toStack(amount))
    }
}
