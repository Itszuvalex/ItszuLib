package com.itszuvalex.itszulib.core.frag

import com.itszuvalex.itszulib.api.Modules
import com.itszuvalex.itszulib.api.adapters.IBlockEntity
import com.itszuvalex.itszulib.api.adapters.IColorable
import com.itszuvalex.itszulib.api.adapters.IItemStack
import com.itszuvalex.itszulib.api.adapters.ILevel
import com.itszuvalex.itszulib.api.adapters.IModule
import com.itszuvalex.itszulib.api.storage.IItemStorage
import com.itszuvalex.itszulib.api.utility.NBTSerializationScope
import com.itszuvalex.itszulib.util.Color
import com.itszuvalex.itszulib.util.IInventoryUtils
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.storage.ValueInput
import net.minecraft.world.level.storage.ValueOutput

/**
 * Gives a block entity a color, exposed on every side through [Modules.COLORABLE]. Setting it saves and syncs to
 * clients. Persisted in every scope as `color` (packed ARGB int).
 */
class FragColorable @JvmOverloads constructor(private var color: Color = Color.TRANSPARENT) :
    BlockEntityFragment<IColorable>(), IColorable {
    override fun getColor(): Color = color

    override fun setColor(color: Color) {
        if (color == this.color) return
        this.color = color
        markDirtyAndSync()
    }

    override fun serializeTo(scope: NBTSerializationScope, output: ValueOutput) = output.putInt(COLOR_TAG, color.toInt())

    override fun deserialize(input: ValueInput, scope: NBTSerializationScope) {
        color = Color(input.getIntOr(COLOR_TAG, 0))
    }

    override fun handlesScope(scope: NBTSerializationScope): Boolean = true

    override fun module(): IModule<IColorable> = Modules.COLORABLE

    override fun faceToModuleMapper(be: IBlockEntity): (Direction?) -> IColorable? = { this }

    override fun name(): String = "Colorable"

    companion object {
        const val COLOR_TAG = "color"
    }
}

/**
 * Drops [storage]'s contents when the block is removed (server side, only when the block actually changes), then
 * clears it.
 */
class FragDropInventory(private val storage: IItemStorage) : InternalBlockEntityFragment() {
    @JvmField
    var shouldDrop = true

    override fun name(): String = NAME

    override fun onRemove(level: ILevel, pos: BlockPos, blockStatePrev: BlockState) {
        if (!shouldDrop) return
        IInventoryUtils.instance.get().dropStorage(level, pos, storage)
        for (i in 0 until storage.size()) storage.setSlot(i, IItemStack.Empty)
    }

    companion object {
        const val NAME = "Drop Inventory"
    }
}
