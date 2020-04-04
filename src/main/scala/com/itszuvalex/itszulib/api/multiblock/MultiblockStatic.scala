package com.itszuvalex.itszulib.api.multiblock

import com.itszuvalex.itszulib.api.ItszuLibModules
import com.itszuvalex.itszulib.api.core.Loc4
import com.itszuvalex.itszulib.api.wrappers.IWorld
import net.minecraft.util.math.BlockPos

class MultiblockStatic(val blockpattern: IBlockPattern) extends IMultiblock {
  override def pattern: IBlockPattern = blockpattern.copy()

  override def form(world: IWorld, pos: BlockPos, pat: IBlockPattern): Boolean = {
    var foundNonMultiblock     = false
    var foundErroredMultiblock = false

    pat.blocksInMatch(world, pos).foreach { x =>
      val te = world.getITileEntity(x)
      if (te == null) foundNonMultiblock = true
      else {
        if (te.hasModule(ItszuLibModules.TILE_MULTIBLOCK, null)) {
          val mb = te.getModule(ItszuLibModules.TILE_MULTIBLOCK, null)
          if (mb == null) foundNonMultiblock = true
          else {
            foundErroredMultiblock = !mb.formMultiBlock(Loc4(world, x), Loc4(world, pos)) || foundErroredMultiblock
          }
        } else foundNonMultiblock = true
      }
    }

    !(foundNonMultiblock || foundErroredMultiblock)
  }

  override def tryForm(world: IWorld, pos: BlockPos, pat: IBlockPattern): Option[Boolean] = {
    if (!pat.matches(world, pos)) None
    else Option(form(world, pos, pat))
  }

  override def break(world: IWorld, pos: BlockPos, pat: IBlockPattern): Boolean = {
    var foundNonMultiblock     = false
    var foundErroredMultiblock = false

    pat.blocksInMatch(world, pos).foreach { x =>
      val te = world.getITileEntity(x)
      if (te == null) foundNonMultiblock = true
      else {
        if (te.hasModule(ItszuLibModules.TILE_MULTIBLOCK, null)) {
          val mb = te.getModule(ItszuLibModules.TILE_MULTIBLOCK, null)
          if (mb == null) foundNonMultiblock = true
          else {
            foundErroredMultiblock = !mb.breakMultiBlock(Loc4(world, pos)) || foundErroredMultiblock
          }
        } else foundNonMultiblock = true
      }
    }

    !(foundNonMultiblock || foundErroredMultiblock)
  }
}
