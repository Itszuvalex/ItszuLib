package com.itszuvalex.itszulib.menu

import net.minecraft.core.BlockPos
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.world.Container
import net.minecraft.world.MenuProvider
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.block.entity.BlockEntity

/**
 * A block's menu: what to open, and which block position to send to the client so its menu finds the same block
 * entity (a multiblock part opens its controller's menu, so this can differ from the clicked block).
 */
interface IMenuHost : MenuProvider {
    fun menuPos(): BlockPos
}

/**
 * Helpers for menus over a block entity.
 */
object BlockMenus {
    /**
     * Client side, in a menu type's factory: the block entity whose position the server wrote (see
     * [IMenuHost.menuPos]), if it is loaded and of type [T].
     */
    @JvmStatic
    inline fun <reified T : BlockEntity> blockEntity(inventory: Inventory, buf: RegistryFriendlyByteBuf): T? =
        inventory.player.level().getBlockEntity(buf.readBlockPos()) as? T

    /**
     * The usual validity check for a block entity's menu: the block entity is still there and the player is within
     * reach.
     */
    @JvmStatic
    fun stillValid(blockEntity: BlockEntity?, player: Player): Boolean =
        blockEntity != null && Container.stillValidBlockEntity(blockEntity, player)
}
