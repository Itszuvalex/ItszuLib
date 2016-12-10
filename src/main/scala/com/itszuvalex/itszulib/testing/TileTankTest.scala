package com.itszuvalex.itszulib.testing

import com.itszuvalex.itszulib.ItszuLib
import com.itszuvalex.itszulib.core.TileEntityBase
import com.itszuvalex.itszulib.core.traits.tile.TileMultiFluidTank
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.util.EnumFacing
import net.minecraftforge.fluids.capability.IFluidTankProperties
import net.minecraftforge.fluids.{Fluid, FluidStack, FluidTank, FluidTankInfo}

/**
  * Created by Alex on 12.10.2015.
  */
class TileTankTest extends TileEntityBase with TileMultiFluidTank {

  override def drain(resource: FluidStack, doDrain: Boolean): FluidStack = null

  override def defaultTanks: Array[FluidTank] = Array(new FluidTank(10000), new FluidTank(5000), new FluidTank(2000))

  override def fill(resource: FluidStack, doFill: Boolean): Int = 0

  override def drain(maxDrain: Int, doDrain: Boolean): FluidStack = null

  def canFill(from: EnumFacing, fluid: Fluid): Boolean = false

  def canDrain(from: EnumFacing, fluid: Fluid): Boolean = false

  override def getTankProperties: Array[IFluidTankProperties] = null

  override def hasDescription: Boolean = true

  override def onSideActivate(par5EntityPlayer: EntityPlayer, side: EnumFacing): Boolean = {
    par5EntityPlayer.openGui(getMod, 0, getWorld, getPos.getX, getPos.getY, getPos.getZ)
    true
  }

  override def getMod: AnyRef = ItszuLib

  override def serverUpdate(): Unit = {
    /*tanks(0).fill(new FluidStack(FluidRegistry.WATER, 5), true)
    tanks(1).fill(new FluidStack(FluidRegistry.LAVA, 2), true)
    setUpdateTanks()*/
    super.serverUpdate()
  }


  override def fill(id: Int, from: EnumFacing, resource: FluidStack, doFill: Boolean): Int = 0

  override def drain(id: Int, from: EnumFacing, resource: FluidStack, doDrain: Boolean): FluidStack = null

  def getTankInfo(from: EnumFacing): Array[FluidTankInfo] = null
}
