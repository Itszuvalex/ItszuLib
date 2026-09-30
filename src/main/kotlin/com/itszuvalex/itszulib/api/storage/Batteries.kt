package com.itszuvalex.itszulib.api.storage

import com.itszuvalex.itszulib.api.adapters.IBattery
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.level.storage.ValueInput
import net.minecraft.world.level.storage.ValueOutput
import net.neoforged.neoforge.transfer.energy.EnergyHandler
import net.neoforged.neoforge.transfer.transaction.Transaction
import kotlin.math.floor

private const val POWER_KEY = "P"
private const val POWER_MAX_KEY = "M"

/**
 * In-memory battery with a fixed capacity. Persists only the current charge (key `P`).
 *
 * @param onChanged Run after every [setStorage] and [setChanged], e.g. the owning block entity's setChanged.
 */
open class PowerBattery @JvmOverloads constructor(
    private val maxPower: Double,
    private val onChanged: Runnable = Runnable {},
) : IBattery {
    private var curPower = 0.0

    override fun storage(): Double = curPower

    override fun setStorage(storage: Double) {
        curPower = storage
        onChanged.run()
    }

    override fun setStorageQuietly(storage: Double) {
        curPower = storage
    }

    override fun setChanged() = onChanged.run()

    override fun maxStorage(): Double = maxPower

    override fun serialize(output: ValueOutput) = output.putDouble(POWER_KEY, storage())

    override fun deserialize(input: ValueInput) = setStorage(input.getDoubleOr(POWER_KEY, 0.0))
}

/**
 * Battery stored in a live CompoundTag (keys `P` charge, `M` capacity), e.g. inside an item's custom data.
 */
open class PowerBatteryNBT(private val nbt: CompoundTag) : IBattery {
    constructor(nbt: CompoundTag, max: Double) : this(nbt) {
        setMaxStorage(max)
    }

    override fun storage(): Double = nbt.getDoubleOr(POWER_KEY, 0.0)

    override fun setStorage(storage: Double) = nbt.putDouble(POWER_KEY, storage)

    override fun maxStorage(): Double = nbt.getDoubleOr(POWER_MAX_KEY, 0.0)

    fun setMaxStorage(max: Double) = nbt.putDouble(POWER_MAX_KEY, max)

    override fun serialize(output: ValueOutput) {
        output.putDouble(POWER_KEY, storage())
        output.putDouble(POWER_MAX_KEY, maxStorage())
    }

    override fun deserialize(input: ValueInput) {
        setStorage(input.getDoubleOr(POWER_KEY, 0.0))
        setMaxStorage(input.getDoubleOr(POWER_MAX_KEY, 0.0))
    }
}

/**
 * Forwards everything to whatever battery [batterySupplier] returns at call time.
 */
open class DynamicIBattery(private val batterySupplier: () -> IBattery) : IBattery {
    override fun room(): Double = batterySupplier().room()
    override fun fill(amt: Double): Double = batterySupplier().fill(amt)
    override fun drain(amt: Double): Double = batterySupplier().drain(amt)
    override fun storage(): Double = batterySupplier().storage()
    override fun setStorage(storage: Double) = batterySupplier().setStorage(storage)
    override fun setStorageQuietly(storage: Double) = batterySupplier().setStorageQuietly(storage)
    override fun setChanged() = batterySupplier().setChanged()
    override fun maxStorage(): Double = batterySupplier().maxStorage()
    override fun serialize(output: ValueOutput) = batterySupplier().serialize(output)
    override fun deserialize(input: ValueInput) = batterySupplier().deserialize(input)
}

/**
 * Battery backed by a NeoForge [EnergyHandler], e.g. another mod's energy capability.
 * Energy is converted 1:1 and truncated to whole units.
 * Mutations open root transactions, so they must not be called while a transaction is open.
 */
open class BatteryEnergyHandler(private val handler: EnergyHandler) : IBattery {
    override fun storage(): Double = handler.amountAsLong.toDouble()

    override fun setStorage(storage: Double) {
        val diff = storage - storage()
        if (diff > 0) fill(diff) else if (diff < 0) drain(-diff)
    }

    override fun maxStorage(): Double = handler.capacityAsLong.toDouble()

    override fun fill(amt: Double): Double = Transaction.openRoot().use { tx ->
        val filled = handler.insert(toInt(amt), tx)
        tx.commit()
        filled.toDouble()
    }

    override fun drain(amt: Double): Double = Transaction.openRoot().use { tx ->
        val drained = handler.extract(toInt(amt), tx)
        tx.commit()
        drained.toDouble()
    }

    /**
     * The backing handler owns its persistence.
     */
    override fun serialize(output: ValueOutput) {}

    override fun deserialize(input: ValueInput) {}

    private fun toInt(amt: Double): Int = floor(amt).coerceIn(0.0, Int.MAX_VALUE.toDouble()).toInt()
}
