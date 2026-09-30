package com.itszuvalex.itszulib.api.storage

import com.itszuvalex.itszulib.api.adapters.IItemStack
import com.mojang.serialization.DynamicOps
import net.minecraft.core.HolderLookup
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.NbtOps
import net.minecraft.nbt.Tag
import net.minecraft.world.level.storage.ValueInput
import net.minecraft.world.level.storage.ValueOutput
import net.neoforged.neoforge.transfer.ResourceHandler
import net.neoforged.neoforge.transfer.item.ItemResource
import net.neoforged.neoforge.transfer.transaction.Transaction

/**
 * Array-backed storage. [get] returns the live stack; mutate it only through this storage (e.g. [setSlot]) so the
 * change listener runs.
 *
 * @param onChanged Run after every [setSlot] and [setChanged], e.g. the owning block entity's setChanged.
 */
open class ItemStorageArray @JvmOverloads constructor(
    items: Array<IItemStack?>,
    private val onChanged: Runnable = Runnable {},
) : IItemStorage {
    private val storage: Array<IItemStack> = Array(items.size) { items[it] ?: IItemStack.Empty }

    @JvmOverloads
    constructor(size: Int, onChanged: Runnable = Runnable {}) : this(arrayOfNulls<IItemStack>(size), onChanged)

    override fun get(index: Int): IItemStack = storage[index]

    override fun size(): Int = storage.size

    override fun setSlot(index: Int, stack: IItemStack) {
        storage[index] = stack
        onChanged.run()
    }

    override fun setSlotQuietly(index: Int, stack: IItemStack) {
        storage[index] = stack
    }

    override fun setChanged() = onChanged.run()
}

/**
 * View of selected [slots] of another storage.
 */
open class ItemStorageSlice(private val storage: IItemStorage, private val slots: IntArray) : IItemStorage {
    override fun get(index: Int): IItemStack = storage.get(slots[index])

    override fun size(): Int = slots.size

    override fun setSlot(index: Int, stack: IItemStack) = storage.setSlot(slots[index], stack)

    override fun setSlotQuietly(index: Int, stack: IItemStack) = storage.setSlotQuietly(slots[index], stack)

    override fun setChanged() = storage.setChanged()
}

/**
 * Concatenation of several storages; index 0 is the first storage's slot 0.
 */
open class ItemStorageAggregate(private val storages: Array<IItemStorage>) : IItemStorage {
    override fun get(index: Int): IItemStack = locate(index)?.let { (s, i) -> s.get(i) } ?: IItemStack.Empty

    override fun size(): Int = storages.sumOf { it.size() }

    override fun setSlot(index: Int, stack: IItemStack) {
        locate(index)?.let { (s, i) -> s.setSlot(i, stack) }
    }

    override fun setSlotQuietly(index: Int, stack: IItemStack) {
        locate(index)?.let { (s, i) -> s.setSlotQuietly(i, stack) }
    }

    override fun setChanged() = storages.forEach { it.setChanged() }

    private fun locate(index: Int): Pair<IItemStorage, Int>? {
        var i = index
        for (storage in storages) {
            if (i < storage.size()) return storage to i
            i -= storage.size()
        }
        return null
    }
}

/**
 * Forwards everything to whatever storage [itemStorageSupplier] returns at call time.
 */
open class DynamicIItemStorage(private val itemStorageSupplier: () -> IItemStorage) : IItemStorage {
    override fun get(index: Int): IItemStack = itemStorageSupplier().get(index)
    override fun size(): Int = itemStorageSupplier().size()
    override fun setSlot(index: Int, stack: IItemStack) = itemStorageSupplier().setSlot(index, stack)
    override fun setSlotQuietly(index: Int, stack: IItemStack) = itemStorageSupplier().setSlotQuietly(index, stack)
    override fun canInsert(index: Int, stack: IItemStack): Boolean = itemStorageSupplier().canInsert(index, stack)
    override fun maxStackSize(index: Int): Int = itemStorageSupplier().maxStackSize(index)
    override fun split(index: Int, amount: Int): IItemStack = itemStorageSupplier().split(index, amount)
    override fun insert(index: Int, stack: IItemStack): IItemStack = itemStorageSupplier().insert(index, stack)
    override fun transferSlotIntoStorageSlot(slot: Int, storage: IItemStorage, targetSlot: Int, amount: Int): Int =
        itemStorageSupplier().transferSlotIntoStorageSlot(slot, storage, targetSlot, amount)
    override fun transferSlotIntoStorage(slot: Int, storage: IItemStorage, amount: Int): Int =
        itemStorageSupplier().transferSlotIntoStorage(slot, storage, amount)
    override fun transferIntoStorage(storage: IItemStorage, amount: Int): Int =
        itemStorageSupplier().transferIntoStorage(storage, amount)
    override fun deserialize(input: ValueInput) = itemStorageSupplier().deserialize(input)
    override fun serialize(output: ValueOutput) = itemStorageSupplier().serialize(output)
    override fun isEmpty(): Boolean = itemStorageSupplier().isEmpty()
    override fun setChanged() = itemStorageSupplier().setChanged()
}

/**
 * Item storage backed by a live CompoundTag, one entry per slot.
 */
open class ItemStorageNBT private constructor(
    private val nbt: CompoundTag,
    private val size: Int,
    private val ops: DynamicOps<Tag>,
    private val onChanged: Runnable,
) : IItemStorage {
    /**
     * Without registry context, items with datapack-registry components (e.g. enchantments) will fail to encode.
     */
    constructor(nbt: CompoundTag, size: Int) : this(nbt, size, NbtOps.INSTANCE, Runnable {})

    /**
     * @param onChanged Run after every [setSlot] and [setChanged], e.g. to write the tag back into an item's data
     * component.
     */
    @JvmOverloads
    constructor(nbt: CompoundTag, size: Int, registries: HolderLookup.Provider, onChanged: Runnable = Runnable {}) :
        this(nbt, size, registries.createSerializationContext(NbtOps.INSTANCE), onChanged)

    override fun get(index: Int): IItemStack {
        val tag = nbt.get(index.toString()) ?: return IItemStack.Empty
        return IItemStack.codec().parse(ops, tag).result().orElse(IItemStack.Empty)
    }

    override fun size(): Int = size

    override fun setSlot(index: Int, stack: IItemStack) {
        setSlotQuietly(index, stack)
        onChanged.run()
    }

    override fun setSlotQuietly(index: Int, stack: IItemStack) {
        if (stack.isEmpty()) {
            nbt.remove(index.toString())
            return
        }
        nbt.put(index.toString(), IItemStack.codec().encodeStart(ops, stack).getOrThrow())
    }

    override fun setChanged() = onChanged.run()
}

/**
 * Item storage backed by a NeoForge item [ResourceHandler], e.g. another mod's inventory capability.
 * Mutations open root transactions, so they must not be called while a transaction is open.
 */
open class ItemStorageResourceHandler(private val handler: ResourceHandler<ItemResource>) : IItemStorage {
    override fun get(index: Int): IItemStack {
        val resource = handler.getResource(index)
        if (resource.isEmpty) return IItemStack.Empty
        return IItemStack.of(resource.toStack(handler.getAmountAsInt(index)))
    }

    override fun size(): Int = handler.size()

    /**
     * Replaces the slot's contents. Leaves the slot unchanged if the handler will not accept the full stack.
     */
    override fun setSlot(index: Int, stack: IItemStack) {
        Transaction.openRoot().use { tx ->
            val current = handler.getResource(index)
            val currentAmount = handler.getAmountAsInt(index)
            if (!current.isEmpty && handler.extract(index, current, currentAmount, tx) != currentAmount) return
            if (!stack.isEmpty()) {
                val resource = ItemResource.of(stack.toMinecraft())
                if (handler.insert(index, resource, stack.stackSize(), tx) != stack.stackSize()) return
            }
            tx.commit()
        }
    }

    override fun canInsert(index: Int, stack: IItemStack): Boolean =
        stack.isEmpty() || handler.isValid(index, ItemResource.of(stack.toMinecraft()))
}
