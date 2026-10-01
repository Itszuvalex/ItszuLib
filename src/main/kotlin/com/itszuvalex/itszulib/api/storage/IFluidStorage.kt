package com.itszuvalex.itszulib.api.storage

import com.itszuvalex.itszulib.api.adapters.IFluidStack
import net.minecraft.world.level.storage.ValueInput
import net.minecraft.world.level.storage.ValueOutput
import net.neoforged.neoforge.common.util.ValueIOSerializable
import kotlin.math.min

/**
 * Indexed fluid tanks with default fill/drain logic. Port of ItszuLib 1.12.2's `IFluidStorage` and
 * `IFluidStorageModifiable` (one interface here: every storage can be written, as [IItemStorage] can).
 *
 * Fill order: tanks already holding the fluid first, then empty tanks. Drains take from the first tank that can be
 * drained, then from further tanks holding the same fluid.
 *
 * Persisted as one entry per non-empty tank, keyed by its index ("0", "1", ...), encoded with [IFluidStack.codec].
 */
interface IFluidStorage : ValueIOSerializable {
    fun size(): Int

    /**
     * @return The tank's contents; never null ([IFluidStack.Empty] when empty). Treat as read-only, and change tanks
     * through [set].
     */
    fun get(index: Int): IFluidStack

    /**
     * Replaces a tank's contents. Storages with a change listener notify it (see [setChanged]).
     */
    fun set(index: Int, stack: IFluidStack)

    /**
     * Replaces a tank's contents without notifying any change listener, e.g. inside a NeoForge transaction.
     */
    fun setQuietly(index: Int, stack: IFluidStack) = set(index, stack)

    /**
     * @return Capacity of the tank in millibuckets.
     */
    fun capacity(index: Int): Int = Int.MAX_VALUE

    fun canFill(index: Int): Boolean = true

    fun canDrain(index: Int): Boolean = true

    fun canFillFluidType(index: Int, resource: IFluidStack): Boolean = !resource.isEmpty() && canFill(index)

    fun canDrainFluidType(index: Int, resource: IFluidStack): Boolean = !resource.isEmpty() && canDrain(index)

    /**
     * @return Amount of [resource] filled (or that would be filled, if not [doFill]).
     */
    fun fill(resource: IFluidStack, doFill: Boolean): Int {
        if (resource.isEmpty()) return 0
        val toFill = resource.amount()
        var filled = 0
        // Tanks already holding the fluid first, then empty tanks.
        for (pass in 0..1) {
            for (i in 0 until size()) {
                if (filled >= toFill) return filled
                val stack = get(i)
                val matches = if (pass == 0) !stack.isEmpty() && resource.isFluidEqual(stack) else stack.isEmpty()
                if (!matches || !canFillFluidType(i, resource)) continue
                val room = capacity(i) - stack.amount()
                val amt = min(toFill - filled, room)
                if (amt <= 0) continue
                if (doFill) set(i, (if (stack.isEmpty()) resource else stack).copyWithAmount(stack.amount() + amt))
                filled += amt
            }
        }
        return filled
    }

    /**
     * Drains [resource]'s fluid, up to its amount, from every tank holding it.
     *
     * @return The fluid drained (or that would be drained, if not [doDrain]); [IFluidStack.Empty] if none.
     */
    fun drain(resource: IFluidStack, doDrain: Boolean): IFluidStack {
        if (resource.isEmpty()) return IFluidStack.Empty
        val drained = drainMatching(resource, resource.amount(), -1, doDrain)
        return resource.copyWithAmount(drained)
    }

    /**
     * Drains up to [maxDrain] of whatever the first drainable non-empty tank holds, topping up from further tanks
     * holding the same fluid.
     *
     * @return The fluid drained (or that would be drained, if not [doDrain]); [IFluidStack.Empty] if none.
     */
    fun drain(maxDrain: Int, doDrain: Boolean): IFluidStack {
        if (maxDrain <= 0) return IFluidStack.Empty
        val first = (0 until size()).firstOrNull { canDrain(it) && !get(it).isEmpty() } ?: return IFluidStack.Empty
        val stack = get(first)
        val fluid = stack.copy()
        val fromFirst = min(maxDrain, stack.amount())
        if (doDrain) set(first, stack.copyWithAmount(stack.amount() - fromFirst))
        // Skip the first tank: when simulating it still holds what was "drained".
        val rest = drainMatching(fluid, maxDrain - fromFirst, first, doDrain)
        return fluid.copyWithAmount(fromFirst + rest)
    }

    private fun drainMatching(fluid: IFluidStack, amount: Int, skip: Int, doDrain: Boolean): Int {
        var drained = 0
        for (i in 0 until size()) {
            if (drained >= amount) break
            if (i == skip) continue
            val stack = get(i)
            if (stack.isEmpty() || !fluid.isFluidEqual(stack) || !canDrainFluidType(i, stack)) continue
            val amt = min(amount - drained, stack.amount())
            if (doDrain) set(i, stack.copyWithAmount(stack.amount() - amt))
            drained += amt
        }
        return drained
    }

    fun isEmpty(): Boolean = (0 until size()).all { get(it).isEmpty() }

    /**
     * Notifies the storage's change listener, if any (e.g. the owning block entity's setChanged).
     */
    fun setChanged() {}

    /**
     * Replaces every tank. [serialize] omits empty tanks, so a missing tank is cleared rather than kept.
     */
    override fun deserialize(input: ValueInput) {
        for (i in 0 until size()) set(i, input.read(i.toString(), IFluidStack.codec()).orElse(IFluidStack.Empty))
    }

    override fun serialize(output: ValueOutput) {
        for (i in 0 until size()) {
            val stack = get(i)
            if (!stack.isEmpty()) output.store(i.toString(), IFluidStack.codec(), stack)
        }
    }

    companion object {
        @JvmField
        val Empty: IFluidStorage = object : IFluidStorage {
            override fun size(): Int = 0
            override fun get(index: Int): IFluidStack = IFluidStack.Empty
            override fun set(index: Int, stack: IFluidStack) {}
            override fun capacity(index: Int): Int = 0
            override fun fill(resource: IFluidStack, doFill: Boolean): Int = 0
            override fun drain(resource: IFluidStack, doDrain: Boolean): IFluidStack = IFluidStack.Empty
            override fun drain(maxDrain: Int, doDrain: Boolean): IFluidStack = IFluidStack.Empty
        }
    }
}

/**
 * Array-backed tanks, each with its own capacity.
 *
 * @param onChanged Run after every [set] and [setChanged], e.g. the owning block entity's setChanged.
 */
open class FluidStorageArray @JvmOverloads constructor(
    private val capacities: IntArray,
    private val onChanged: Runnable = Runnable {},
) : IFluidStorage {
    private val tanks: Array<IFluidStack> = Array(capacities.size) { IFluidStack.Empty }

    /**
     * [size] tanks of [capacity] each.
     */
    @JvmOverloads
    constructor(size: Int, capacity: Int, onChanged: Runnable = Runnable {}) : this(IntArray(size) { capacity }, onChanged)

    override fun size(): Int = tanks.size

    override fun get(index: Int): IFluidStack = tanks[index]

    override fun set(index: Int, stack: IFluidStack) {
        tanks[index] = if (stack.isEmpty()) IFluidStack.Empty else stack
        onChanged.run()
    }

    override fun setQuietly(index: Int, stack: IFluidStack) {
        tanks[index] = if (stack.isEmpty()) IFluidStack.Empty else stack
    }

    override fun capacity(index: Int): Int = capacities[index]

    override fun setChanged() = onChanged.run()
}

/**
 * View of selected [tanks] of another storage.
 */
open class FluidStorageSlice(private val storage: IFluidStorage, private val tanks: IntArray) : IFluidStorage {
    override fun size(): Int = tanks.size

    override fun get(index: Int): IFluidStack = storage.get(tanks[index])

    override fun set(index: Int, stack: IFluidStack) = storage.set(tanks[index], stack)

    override fun setQuietly(index: Int, stack: IFluidStack) = storage.setQuietly(tanks[index], stack)

    override fun capacity(index: Int): Int = storage.capacity(tanks[index])

    override fun canFill(index: Int): Boolean = storage.canFill(tanks[index])

    override fun canDrain(index: Int): Boolean = storage.canDrain(tanks[index])

    override fun canFillFluidType(index: Int, resource: IFluidStack): Boolean = storage.canFillFluidType(tanks[index], resource)

    override fun canDrainFluidType(index: Int, resource: IFluidStack): Boolean = storage.canDrainFluidType(tanks[index], resource)

    override fun setChanged() = storage.setChanged()
}

/**
 * Forwards to whatever storage [getter] returns at the time of each call, e.g. a multiblock controller's.
 */
open class DynamicIFluidStorage(private val getter: () -> IFluidStorage) : IFluidStorage {
    override fun size(): Int = getter().size()

    override fun get(index: Int): IFluidStack = getter().get(index)

    override fun set(index: Int, stack: IFluidStack) = getter().set(index, stack)

    override fun setQuietly(index: Int, stack: IFluidStack) = getter().setQuietly(index, stack)

    override fun capacity(index: Int): Int = getter().capacity(index)

    override fun canFill(index: Int): Boolean = getter().canFill(index)

    override fun canDrain(index: Int): Boolean = getter().canDrain(index)

    override fun canFillFluidType(index: Int, resource: IFluidStack): Boolean = getter().canFillFluidType(index, resource)

    override fun canDrainFluidType(index: Int, resource: IFluidStack): Boolean = getter().canDrainFluidType(index, resource)

    override fun fill(resource: IFluidStack, doFill: Boolean): Int = getter().fill(resource, doFill)

    override fun drain(resource: IFluidStack, doDrain: Boolean): IFluidStack = getter().drain(resource, doDrain)

    override fun drain(maxDrain: Int, doDrain: Boolean): IFluidStack = getter().drain(maxDrain, doDrain)

    override fun setChanged() = getter().setChanged()

    override fun serialize(output: ValueOutput) = getter().serialize(output)

    override fun deserialize(input: ValueInput) = getter().deserialize(input)
}
