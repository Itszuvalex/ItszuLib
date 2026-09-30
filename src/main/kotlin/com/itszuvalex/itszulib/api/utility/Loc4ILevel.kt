package com.itszuvalex.itszulib.api.utility

import com.itszuvalex.itszulib.api.adapters.IBlockEntity
import com.itszuvalex.itszulib.api.adapters.ILevel
import net.minecraft.core.BlockPos
import net.minecraft.resources.Identifier
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.entity.BlockEntity

class Loc4ILevel(private val ilevel: ILevel, pos: BlockPos) : Loc4(pos) {
    val level: Level get() = ilevel.toMinecraft()

    override val dimensionId: Identifier get() = ilevel.dimensionLocation()

    override fun getOffset(x: Int, y: Int, z: Int): Loc4 = of(ilevel, BlockPos(this.x + x, this.y + y, this.z + z))

    override fun copy(): Loc4 = of(ilevel, BlockPos(x, y, z))

    override fun getIBlockEntity(force: Boolean): IBlockEntity? =
        if (force || ilevel.isLoaded(pos)) ilevel.getIBlockEntity(pos) else null

    override fun getBlockEntity(force: Boolean): BlockEntity? = getIBlockEntity(force)?.toMinecraft()
}
