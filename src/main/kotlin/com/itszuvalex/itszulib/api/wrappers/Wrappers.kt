package com.itszuvalex.itszulib.api.wrappers

import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.google.common.cache.LoadingCache
import com.itszuvalex.itszulib.api.adapters.IBattery
import com.itszuvalex.itszulib.api.adapters.IBlockEntity
import com.itszuvalex.itszulib.api.adapters.IItemStack
import com.itszuvalex.itszulib.api.adapters.ILevel
import com.itszuvalex.itszulib.api.adapters.IModule
import com.itszuvalex.itszulib.api.adapters.IModuleProvider
import com.itszuvalex.itszulib.api.storage.IItemStorage
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.component.DataComponentPatch
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.Identifier
import net.minecraft.resources.ResourceKey
import net.minecraft.world.Container
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.entity.BlockEntity
import net.neoforged.neoforge.transfer.ResourceHandler
import net.neoforged.neoforge.transfer.TransferPreconditions
import net.neoforged.neoforge.transfer.access.ItemAccess
import net.neoforged.neoforge.transfer.energy.EnergyHandler
import net.neoforged.neoforge.transfer.item.ItemResource
import net.neoforged.neoforge.transfer.item.VanillaContainerWrapper
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal
import net.neoforged.neoforge.transfer.transaction.TransactionContext
import kotlin.math.floor
import kotlin.math.min

/**
 * Keys by identity (weak keys), so there is one wrapper per in-memory object regardless of `equals`. Meant for
 * widely used, long-lived objects such as Levels; not for ItemStacks.
 *
 * @param C Core class to be wrapped
 * @param W Wrapper class
 */
class WrapperCache<C : Any, W : Any>(cap: Int, loader: (C) -> W) {
    private val implCache: LoadingCache<C, W> = CacheBuilder.newBuilder()
        .initialCapacity(cap).maximumSize(cap.toLong()).concurrencyLevel(1).weakKeys()
        .build(object : CacheLoader<C, W>() {
            override fun load(key: C): W = loader(key)
        })

    fun get(core: C): W = implCache.getUnchecked(core)
}

class WrapperLevel(private val level: Level) : ILevel {
    override fun isClientSide(): Boolean = level.isClientSide
    override fun dimension(): ResourceKey<Level> = level.dimension()
    override fun dimensionLocation(): Identifier = dimension().identifier()
    override fun toMinecraft(): Level = level
    override fun isLoaded(pos: BlockPos): Boolean = level.isLoaded(pos)
    override fun getIBlockEntity(pos: BlockPos): IBlockEntity? = level.getBlockEntity(pos)?.let(IBlockEntity::of)
    override fun setIBlockEntity(entity: IBlockEntity) = level.setBlockEntity(entity.toMinecraft())
    override fun setBlockEntity(entity: BlockEntity) = level.setBlockEntity(entity)

    companion object {
        private const val CACHE_SIZE = 16
        private val cache = WrapperCache<Level, WrapperLevel>(CACHE_SIZE, ::WrapperLevel)

        @JvmStatic
        fun of(level: Level): WrapperLevel = cache.get(level)
    }
}

/**
 * Resolves modules on an arbitrary BlockEntity through the NeoForge block capability system.
 */
class WrapperCapabilityProvider(private val blockEntity: BlockEntity) : IModuleProvider {
    override fun <T : Any> getModule(module: IModule<T>, side: Direction?): T? {
        val level = blockEntity.level ?: return null
        val cap = module.blockCapability ?: return null
        return level.getCapability(cap, blockEntity.blockPos, blockEntity.blockState, blockEntity, side)
    }
}

class WrapperBlockEntity(private val entity: BlockEntity) : IBlockEntity {
    private val capabilityProvider = WrapperCapabilityProvider(entity)

    override fun getBlockPos(): BlockPos = entity.blockPos
    override fun toMinecraft(): BlockEntity = entity
    override fun <T : Any> getModule(module: IModule<T>, side: Direction?): T? = capabilityProvider.getModule(module, side)
}

class WrapperVanillaItemStack(private val stack: ItemStack) : IItemStack {
    override fun item(): Identifier = BuiltInRegistries.ITEM.getKey(stack.item)
    override fun stackSize(): Int = stack.count
    override fun setStackSize(size: Int) {
        stack.count = size
    }
    override fun stackSizeMax(): Int = stack.maxStackSize
    override fun damage(): Int = stack.damageValue
    override fun setDamage(damage: Int) {
        stack.damageValue = damage
    }
    override fun damageMax(): Int = stack.maxDamage
    override fun components(): DataComponentPatch = stack.componentsPatch
    override fun toMinecraft(): ItemStack = stack
    override fun isEmpty(): Boolean = stack.isEmpty
    override fun copy(): IItemStack = WrapperVanillaItemStack(stack.copy())
    override fun isItemEqual(other: IItemStack): Boolean = ItemStack.isSameItemSameComponents(stack, other.toMinecraft())
    override fun <T : Any> getModule(module: IModule<T>, side: Direction?): T? =
        module.itemCapability?.getCapability(stack, ItemAccess.forStack(stack))
    override fun toString(): String = "WrapperVanillaItemStack[$stack]"
}

/**
 * Presents an [IItemStorage] as a vanilla [Container], for menus and for NeoForge's [VanillaContainerWrapper].
 */
class WrapperContainerIItemStorage(private val storage: IItemStorage) : Container {
    override fun getContainerSize(): Int = storage.size()
    override fun isEmpty(): Boolean = storage.isEmpty()
    override fun getItem(slot: Int): ItemStack = storage.get(slot).toMinecraft()
    override fun removeItem(slot: Int, count: Int): ItemStack = storage.split(slot, count).toMinecraft()
    override fun removeItemNoUpdate(slot: Int): ItemStack {
        val prev = storage.get(slot)
        storage.setSlot(slot, IItemStack.Empty)
        return prev.toMinecraft()
    }
    override fun setItem(slot: Int, stack: ItemStack) = storage.setSlot(slot, IItemStack.of(stack))

    /**
     * Inside a transaction, write without notifying; NeoForge calls [setChanged] once on root commit.
     */
    override fun setItem(slot: Int, stack: ItemStack, insideTransaction: Boolean) {
        if (insideTransaction) storage.setSlotQuietly(slot, IItemStack.of(stack)) else setItem(slot, stack)
    }
    override fun setChanged() = storage.setChanged()
    override fun canPlaceItem(slot: Int, stack: ItemStack): Boolean = storage.canInsert(slot, IItemStack.of(stack))
    override fun stillValid(player: Player): Boolean = false
    override fun clearContent() {
        for (i in 0 until storage.size()) storage.setSlot(i, IItemStack.Empty)
    }
}

/**
 * Exposes an [IItemStorage] as a NeoForge item [ResourceHandler], e.g. for the item BLOCK capability.
 * Transactions are handled by NeoForge's [VanillaContainerWrapper].
 */
object WrapperResourceHandlerIItemStorage {
    @JvmStatic
    fun of(storage: IItemStorage): ResourceHandler<ItemResource> = VanillaContainerWrapper.of(WrapperContainerIItemStorage(storage))
}

/**
 * Exposes an [IBattery] as a NeoForge [EnergyHandler], e.g. for the energy BLOCK capability.
 * Energy is converted 1:1 and truncated to whole units.
 */
class WrapperEnergyHandlerIBattery(private val battery: IBattery) : SnapshotJournal<Double>(), EnergyHandler {
    override fun getAmountAsLong(): Long = floor(battery.storage()).toLong()

    override fun getCapacityAsLong(): Long = floor(battery.maxStorage()).toLong()

    override fun insert(amount: Int, transaction: TransactionContext): Int {
        TransferPreconditions.checkNonNegative(amount)
        val toFill = min(amount.toDouble(), floor(battery.room())).toInt()
        if (toFill <= 0) return 0
        updateSnapshots(transaction)
        // Quiet while the transaction is open; the battery is notified once, on root commit.
        battery.setStorageQuietly(battery.storage() + toFill)
        return toFill
    }

    override fun extract(amount: Int, transaction: TransactionContext): Int {
        TransferPreconditions.checkNonNegative(amount)
        val toDrain = min(amount.toDouble(), floor(battery.storage())).toInt()
        if (toDrain <= 0) return 0
        updateSnapshots(transaction)
        battery.setStorageQuietly(battery.storage() - toDrain)
        return toDrain
    }

    override fun createSnapshot(): Double = battery.storage()

    override fun revertToSnapshot(snapshot: Double) = battery.setStorageQuietly(snapshot)

    override fun onRootCommit(originalState: Double) = battery.setChanged()
}
