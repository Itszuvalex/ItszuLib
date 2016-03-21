package com.itszuvalex.itszulib.testing

import com.itszuvalex.itszulib.ItszuLib
import com.itszuvalex.itszulib.core.TileEntityBase
import com.itszuvalex.itszulib.core.traits.tile.TileMultiFluidTank
import net.minecraft.entity.player.EntityPlayer
import net.minecraftforge.common.util.ForgeDirection
import net.minecraftforge.fluids.{Fluid, FluidStack, FluidTank}

/**
  * Created by Alex on 12.10.2015.
  */
class TileTankTest extends TileEntityBase with TileMultiFluidTank {

  override def drain(from: ForgeDirection, resource: FluidStack, doDrain: Boolean): FluidStack = null

  override def defaultTanks: Array[FluidTank] = Array(new FluidTank(10000), new FluidTank(5000), new FluidTank(2000))

  override def fill(from: ForgeDirection, resource: FluidStack, doFill: Boolean): Int = 0

  override def drain(from: ForgeDirection, maxDrain: Int, doDrain: Boolean): FluidStack = null

  override def canFill(from: ForgeDirection, fluid: Fluid): Boolean = false

  override def canDrain(from: ForgeDirection, fluid: Fluid): Boolean = false

  override def hasDescription: Boolean = true

  override def onSideActivate(player: EntityPlayer, side: Int): Boolean = {
    player.openGui(getMod, 0, worldObj, xCoord, yCoord, zCoord)
    true
  }

  override def getMod: AnyRef = ItszuLib

  override def serverUpdate(): Unit = {
    /*tanks(0).fill(new FluidStack(FluidRegistry.WATER, 5), true)
    tanks(1).fill(new FluidStack(FluidRegistry.LAVA, 2), true)
    setUpdateTanks()*/
    super.serverUpdate()
  }
}
