package com.itszuvalex.itszulib.core

import com.itszuvalex.itszulib.api.Components
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.TooltipFlag
import net.minecraft.world.item.component.TooltipDisplay
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.EntityBlock
import java.util.function.Consumer

/**
 * A block item that shows what its block keeps ([BreakBehavior.KEEP]): for each fragment that keeps contents, how much
 * is used of how much it can hold ([IBreakContents.describe]). The same lines show on a fresh item (nothing used, so
 * how much it can hold) and on one carrying contents (`itszulib:fragment_data`).
 *
 * It builds a block entity that is not placed anywhere, applies the item's data to it and asks it, so whatever the
 * fragments can describe shows without a second description to keep in step.
 */
open class ContentBlockItem(block: Block, properties: Properties) : BlockItem(block, properties) {
    override fun appendHoverText(stack: ItemStack, context: TooltipContext, display: TooltipDisplay, builder: Consumer<Component>, flag: TooltipFlag) {
        super.appendHoverText(stack, context, display, builder, flag)
        val block = block as? EntityBlock ?: return
        val be = block.newBlockEntity(BlockPos.ZERO, this.block.defaultBlockState()) as? BlockEntityCore ?: return
        val registries = context.registries()
        val data = stack.get(Components.FRAGMENT_DATA.get())
        if (data != null && registries != null) be.applyItemData(data, registries)
        be.contentFragments().filter { it.breakBehavior == BreakBehavior.KEEP }.flatMap { it.describe() }.forEach(builder::accept)
    }
}
