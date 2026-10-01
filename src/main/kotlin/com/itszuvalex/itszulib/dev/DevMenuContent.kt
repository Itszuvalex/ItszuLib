package com.itszuvalex.itszulib.dev

import com.itszuvalex.itszulib.ItszuLib
import com.itszuvalex.itszulib.api.Modules
import com.itszuvalex.itszulib.api.adapters.IModule
import com.itszuvalex.itszulib.api.storage.IItemStorage
import com.itszuvalex.itszulib.core.BlockEntityCore
import com.itszuvalex.itszulib.core.EntityBlockCore
import com.itszuvalex.itszulib.core.TileNetwork
import com.itszuvalex.itszulib.core.frag.FragNetworkedWire
import com.itszuvalex.itszulib.menu.BlockMenus
import com.itszuvalex.itszulib.menu.MenuCore
import com.itszuvalex.itszulib.menu.MenuSyncs
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockBehaviour
import net.minecraft.world.level.block.state.BlockState
import net.neoforged.fml.LogicalSide

/**
 * The dev blocks' menu. Over a [DevMachineBlockEntity]: an input slot, a take-only output slot, the tank (synced) and
 * action 0 (cycle the item storage on face `data` forward). Over a [DevMultiblockBlockEntity]: its inventory slot and
 * the controller's tick count (synced). Then the player's inventory.
 */
class DevMenu(containerId: Int, inventory: Inventory, @JvmField val blockEntity: BlockEntityCore?) :
    MenuCore(DevContent.DEV_MENU.get(), containerId, inventory.player) {
    /**
     * Client copy of the multiblock tick count.
     */
    var ticks = 0

    init {
        val storage = blockEntity?.getModule(Modules.ITEM_STORAGE, null) ?: IItemStorage.Empty
        when (blockEntity) {
            is DevMachineBlockEntity -> {
                addStorageSlots(storage, 44, 35, count = 1)
                addStorageSlots(storage, 116, 35, first = 1, count = 1, output = true)
                addSync(MenuSyncs.fluid(blockEntity.tanks, 0))
            }
            is DevMultiblockBlockEntity -> {
                addStorageSlots(storage, 80, 35)
                addSync(MenuSyncs.int({ blockEntity.mbState.get()?.count ?: 0 }, { ticks = it }))
            }
            else -> {}
        }
        addPlayerInventorySlots(inventory)
    }

    override fun stillValid(player: Player): Boolean = BlockMenus.stillValid(blockEntity, player)

    override fun handleAction(player: Player, action: Int, data: Int): Boolean {
        val machine = blockEntity as? DevMachineBlockEntity ?: return false
        if (action != ACTION_CYCLE_STORAGE || data !in Direction.entries.indices) return false
        machine.itemConfig.update { it.cycleRelativeFacingStorageForward(Direction.entries[data]) }
        return true
    }

    companion object {
        const val ACTION_CYCLE_STORAGE = 0
    }
}

/**
 * A dev wire's network.
 */
class DevWireNetwork(id: Int) : TileNetwork<DevWire, DevWireNetwork>(id, LogicalSide.SERVER) {
    override fun create(): DevWireNetwork = DevWireNetwork(nextId())

    override fun networkModule(): IModule<DevWire> = DevContent.DEV_WIRE_MODULE

    companion object {
        fun nextId(): Int = ItszuLib.NETWORK_MANAGER.get(LogicalSide.SERVER)?.getNextID() ?: 0
    }
}

class DevWire : FragNetworkedWire<DevWire, DevWireNetwork>({ DevWireNetwork(DevWireNetwork.nextId()) }) {
    override fun module(): IModule<DevWire> = DevContent.DEV_WIRE_MODULE
}

/**
 * A wire that joins neighbouring dev wires into a [DevWireNetwork].
 */
class DevWireBlock(properties: BlockBehaviour.Properties) :
    EntityBlockCore<DevWireBlockEntity>(properties, { DevContent.DEV_WIRE_BLOCK_ENTITY.get() }) {
    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity = DevWireBlockEntity(pos, state)
}

class DevWireBlockEntity(pos: BlockPos, state: BlockState) : BlockEntityCore(DevContent.DEV_WIRE_BLOCK_ENTITY.get(), pos, state) {
    @JvmField
    val wire = DevWire()

    init {
        fragList.addFragment(wire)
    }
}
