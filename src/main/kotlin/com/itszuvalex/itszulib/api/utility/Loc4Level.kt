package com.itszuvalex.itszulib.api.utility

import com.itszuvalex.itszulib.api.adapters.IBlockEntity
import net.minecraft.core.BlockPos
import net.minecraft.resources.Identifier
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.entity.BlockEntity

class Loc4Level(val level: Level, pos: BlockPos) : Loc4(pos) {
    override val dimensionId: Identifier get() = level.dimension().identifier()

    override fun getOffset(x: Int, y: Int, z: Int): Loc4 = of(level, BlockPos(this.x + x, this.y + y, this.z + z))

    override fun copy(): Loc4 = of(level, BlockPos(x, y, z))

    override fun getIBlockEntity(force: Boolean): IBlockEntity? = getBlockEntity(force)?.let(IBlockEntity::of)

    override fun getBlockEntity(force: Boolean): BlockEntity? =
        if (force || level.isLoaded(pos)) level.getBlockEntity(pos) else null
}
