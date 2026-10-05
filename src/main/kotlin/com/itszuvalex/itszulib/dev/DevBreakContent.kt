package com.itszuvalex.itszulib.dev

import com.itszuvalex.itszulib.api.storage.FluidStorageArray
import com.itszuvalex.itszulib.api.storage.ItemStorageArray
import com.itszuvalex.itszulib.api.storage.PowerBattery
import com.itszuvalex.itszulib.core.BlockEntityCore
import com.itszuvalex.itszulib.core.BreakBehavior
import com.itszuvalex.itszulib.core.EntityBlockCore
import com.itszuvalex.itszulib.core.frag.FragEnergyStorage
import com.itszuvalex.itszulib.core.frag.FragFluidStorage
import com.itszuvalex.itszulib.core.frag.FragItemStorage
import com.itszuvalex.itszulib.core.frag.addEnergyStorage
import com.itszuvalex.itszulib.core.frag.addFluidStorage
import com.itszuvalex.itszulib.core.frag.addItemStorage
import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockBehaviour
import net.minecraft.world.level.block.state.BlockState

/**
 * A chest: item storage with the default break behaviour, [BreakBehavior.DROP].
 */
class DevChestBlock(properties: BlockBehaviour.Properties) :
    EntityBlockCore<DevChestBlockEntity>(properties, { DevContent.DEV_CHEST_BLOCK_ENTITY.get() }) {
    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity = DevChestBlockEntity(pos, state)
}

class DevChestBlockEntity(pos: BlockPos, state: BlockState) : BlockEntityCore(DevContent.DEV_CHEST_BLOCK_ENTITY.get(), pos, state) {
    init {
        fragList.addItemStorage(FragItemStorage(ItemStorageArray(4) { markDirty() }))
    }
}

/**
 * A shulker box: items, fluid and energy that stay with the block ([BreakBehavior.KEEP]).
 */
class DevKeeperBlock(properties: BlockBehaviour.Properties) :
    EntityBlockCore<DevKeeperBlockEntity>(properties, { DevContent.DEV_KEEPER_BLOCK_ENTITY.get() }) {
    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity = DevKeeperBlockEntity(pos, state)
}

class DevKeeperBlockEntity(pos: BlockPos, state: BlockState) : BlockEntityCore(DevContent.DEV_KEEPER_BLOCK_ENTITY.get(), pos, state) {
    init {
        fragList.addItemStorage(FragItemStorage(ItemStorageArray(4) { markDirty() }, breakBehavior = BreakBehavior.KEEP))
        fragList.addFluidStorage(FragFluidStorage(FluidStorageArray(2, 4000) { markDirty() }, breakBehavior = BreakBehavior.KEEP))
        fragList.addEnergyStorage(FragEnergyStorage(PowerBattery(1000.0) { markDirty() }, breakBehavior = BreakBehavior.KEEP))
    }
}
