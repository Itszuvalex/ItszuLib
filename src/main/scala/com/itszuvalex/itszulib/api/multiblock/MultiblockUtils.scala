package com.itszuvalex.itszulib.api.multiblock

import com.itszuvalex.itszulib.api.ItszuLibModules
import com.itszuvalex.itszulib.api.core.Loc4
import net.minecraft.util.EnumFacing

object MultiblockUtils {

  def isLoc4FacingInMultiblock(loc: Loc4, facing: EnumFacing, info: MultiBlockInfo): Boolean = {
    val nloc = loc.getOffset(facing)
    nloc.getITileEntity(false) match {
      case None => false
      case Some(te) if te.hasModule(ItszuLibModules.TILE_MULTIBLOCK, null) =>
        val multiblock = te.getModule(ItszuLibModules.TILE_MULTIBLOCK, null)
        if (info.isValidMultiBlock && multiblock.isValidMultiBlock) {
          info.controller.get == multiblock.controller.get
        } else false
      case _ => false
    }
  }

}
