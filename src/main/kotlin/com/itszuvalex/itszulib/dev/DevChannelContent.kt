package com.itszuvalex.itszulib.dev

import com.itszuvalex.itszulib.ItszuLib
import com.itszuvalex.itszulib.channel.FragChannel
import com.itszuvalex.itszulib.core.BlockEntityCore
import com.itszuvalex.itszulib.core.EntityBlockCore
import com.itszuvalex.itszulib.core.frag.FragMenu
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockBehaviour
import net.minecraft.world.level.block.state.BlockState

/**
 * A block that joins two kinds of channel, to try the channel framework: using it opens a menu whose channel tab picks
 * the channel for power and for items.
 */
class DevChannelBlock(properties: BlockBehaviour.Properties) :
    EntityBlockCore<DevChannelBlockEntity>(properties, { DevContent.DEV_CHANNEL_BLOCK_ENTITY.get() }) {
    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity = DevChannelBlockEntity(pos, state)
}

class DevChannelBlockEntity(pos: BlockPos, state: BlockState) : BlockEntityCore(DevContent.DEV_CHANNEL_BLOCK_ENTITY.get(), pos, state) {
    @JvmField
    val channels = FragChannel(listOf(POWER, ITEMS))

    init {
        fragList.addFragment(channels)
        fragList.addFragment(FragMenu(Component.literal("Dev channels"), { id, inventory, _ -> DevMenu(id, inventory, this) }))
    }

    companion object {
        @JvmField
        val POWER: Identifier = Identifier.fromNamespaceAndPath(ItszuLib.ID, "dev_power")

        @JvmField
        val ITEMS: Identifier = Identifier.fromNamespaceAndPath(ItszuLib.ID, "dev_items")
    }
}
