package com.itszuvalex.itszulib.menu

import com.itszuvalex.itszulib.api.adapters.IBattery
import com.itszuvalex.itszulib.api.adapters.IFluidStack
import com.itszuvalex.itszulib.api.storage.IFluidStorage
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.neoforged.neoforge.fluids.FluidStack

/**
 * One value kept in step between a server menu and its client copy, for values that are not slots (power, progress,
 * tank contents, ...). Port of ItszuLib 1.12.2's `ISync`/`SyncBase`, with a [StreamCodec] instead of NBT.
 *
 * On the server, [poll] reads the value and reports whether it changed since the last poll (the first poll always
 * reports a change). On the client, [decode] reads a sent value and hands it to the setter.
 *
 * @param copy Snapshot of a value for the change check; needed for mutable values (e.g. fluid stacks).
 */
class MenuSync<T : Any>(
    private val getter: () -> T,
    private val setter: (T) -> Unit,
    @JvmField val codec: StreamCodec<in RegistryFriendlyByteBuf, T>,
    private val equal: (T, T) -> Boolean = { a, b -> a == b },
    private val copy: (T) -> T = { it },
) {
    private var last: T? = null

    /**
     * Index in its menu, assigned by [MenuCore.addSync].
     */
    var index: Int = -1
        internal set

    /**
     * Reads the value; true if it differs from the last polled (or decoded) value.
     */
    fun poll(): Boolean {
        val value = getter()
        val previous = last
        if (previous != null && equal(previous, value)) return false
        last = copy(value)
        return true
    }

    /**
     * Writes the last polled value (or the current one if never polled).
     */
    fun encode(buf: RegistryFriendlyByteBuf) = codec.encode(buf, last ?: getter())

    fun decode(buf: RegistryFriendlyByteBuf) {
        val value = codec.decode(buf)
        last = value
        setter(value)
    }

    /**
     * Forgets the last value, so the next [poll] reports a change.
     */
    fun reset() {
        last = null
    }
}

/**
 * Factories for common [MenuSync]s.
 */
object MenuSyncs {
    @JvmField
    val INT: StreamCodec<RegistryFriendlyByteBuf, Int> = ByteBufCodecs.VAR_INT.cast()

    @JvmField
    val LONG: StreamCodec<RegistryFriendlyByteBuf, Long> = ByteBufCodecs.VAR_LONG.cast()

    @JvmField
    val FLOAT: StreamCodec<RegistryFriendlyByteBuf, Float> = ByteBufCodecs.FLOAT.cast()

    @JvmField
    val DOUBLE: StreamCodec<RegistryFriendlyByteBuf, Double> = ByteBufCodecs.DOUBLE.cast()

    @JvmField
    val BOOLEAN: StreamCodec<RegistryFriendlyByteBuf, Boolean> = ByteBufCodecs.BOOL.cast()

    @JvmField
    val STRING: StreamCodec<RegistryFriendlyByteBuf, String> = ByteBufCodecs.STRING_UTF8.cast()

    @JvmField
    val FLUID: StreamCodec<RegistryFriendlyByteBuf, IFluidStack> =
        FluidStack.OPTIONAL_STREAM_CODEC.map({ IFluidStack.of(it) }, { it.toMinecraft() })

    @JvmStatic
    fun int(getter: () -> Int, setter: (Int) -> Unit) = MenuSync(getter, setter, INT)

    @JvmStatic
    fun long(getter: () -> Long, setter: (Long) -> Unit) = MenuSync(getter, setter, LONG)

    @JvmStatic
    fun float(getter: () -> Float, setter: (Float) -> Unit) = MenuSync(getter, setter, FLOAT)

    @JvmStatic
    fun double(getter: () -> Double, setter: (Double) -> Unit) = MenuSync(getter, setter, DOUBLE)

    @JvmStatic
    fun boolean(getter: () -> Boolean, setter: (Boolean) -> Unit) = MenuSync(getter, setter, BOOLEAN)

    @JvmStatic
    fun string(getter: () -> String, setter: (String) -> Unit) = MenuSync(getter, setter, STRING)

    /**
     * Syncs tank [index] of [storage] (port of 1.12.2's `SyncFluidStorageFluidStack`). Compared by fluid and amount.
     */
    @JvmStatic
    fun fluid(storage: IFluidStorage, index: Int) = MenuSync(
        { storage.get(index) },
        { storage.setQuietly(index, it) },
        FLUID,
        { a, b -> a.amount() == b.amount() && a.isFluidEqual(b) },
        { it.copy() },
    )

    /**
     * Syncs a battery's stored energy (the client copy is set quietly).
     */
    @JvmStatic
    fun energy(battery: IBattery) = MenuSync({ battery.storage() }, { battery.setStorageQuietly(it) }, DOUBLE)
}
