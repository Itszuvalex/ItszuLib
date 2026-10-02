package com.itszuvalex.itszulib.api.utility

import com.itszuvalex.itszulib.api.adapters.IBlockEntity
import net.minecraft.core.BlockPos
import net.minecraft.resources.Identifier
import net.minecraft.world.level.block.entity.BlockEntity

/**
 * A location known only by dimension id; world lookups always return null.
 */
class Loc4Indirect(private val worldId: Identifier, pos: BlockPos) : Loc4(pos) {
    override val dimensionId: Identifier get() = worldId

    override fun getOffset(x: Int, y: Int, z: Int): Loc4 = of(worldId, BlockPos(this.x + x, this.y + y, this.z + z))

    override fun copy(): Loc4 = of(worldId, BlockPos(x, y, z))

    override fun getIBlockEntity(force: Boolean): IBlockEntity? = null

    override fun getBlockEntity(force: Boolean): BlockEntity? = null
}
