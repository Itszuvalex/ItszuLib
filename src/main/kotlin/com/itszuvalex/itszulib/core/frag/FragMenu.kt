package com.itszuvalex.itszulib.core.frag

import com.itszuvalex.itszulib.api.Modules
import com.itszuvalex.itszulib.api.adapters.IBlockEntity
import com.itszuvalex.itszulib.api.adapters.IModule
import com.itszuvalex.itszulib.menu.IMenuHost
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.AbstractContainerMenu

/**
 * Gives a block a menu, opened when the block is used (see [com.itszuvalex.itszulib.core.EntityBlockCore]). Port of
 * ItszuLib 1.12.2's `ModuleGui`, and of `ModuleMultiblockGui` when [multiblock] is given: then the menu is only
 * available while the block is part of a formed structure. Each member opens its own menu; menus over the structure
 * read its shared state ([FragMultiblockPart.sharedState]).
 *
 * Client side, the menu type's factory reads the position written by [menuPos] (see
 * [com.itszuvalex.itszulib.menu.BlockMenus.blockEntity]).
 *
 * @param factory Creates the server-side menu.
 */
class FragMenu @JvmOverloads constructor(
    private val title: Component,
    private val factory: (containerId: Int, inventory: Inventory, player: Player) -> AbstractContainerMenu,
    private val multiblock: FragMultiblockPart? = null,
) : BlockEntityFragment<IMenuHost>(), IMenuHost {
    override fun name(): String = NAME

    override fun module(): IModule<IMenuHost> = Modules.MENU

    override fun faceToModuleMapper(be: IBlockEntity): (Direction?) -> IMenuHost? = { menu() }

    /**
     * The menu this block opens now, or null.
     */
    fun menu(): IMenuHost? {
        val part = multiblock ?: return this
        return if (part.isFormed) this else null
    }

    override fun menuPos(): BlockPos = host?.blockEntity()?.getBlockPos() ?: BlockPos.ZERO

    override fun getDisplayName(): Component = title

    override fun createMenu(containerId: Int, inventory: Inventory, player: Player): AbstractContainerMenu =
        factory(containerId, inventory, player)

    companion object {
        const val NAME = "Menu"
    }
}
