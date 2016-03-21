package com.itszuvalex.itszulib.core.traits.tile

import com.itszuvalex.itszulib.api.core.Saveable
import com.itszuvalex.itszulib.core.TileEntityBase
import com.itszuvalex.itszulib.network.PacketHandler
import com.itszuvalex.itszulib.network.messages.MessageFluidTankUpdate
import net.minecraftforge.common.util.ForgeDirection
import net.minecraftforge.fluids._

/**
  * Created by Chris on 11/30/2014.
  */
trait TileFluidTank extends TileEntityBase with IFluidHandler {
  @Saveable var tank = defaultTank
  var updateNeeded: Boolean = false

  def defaultTank: FluidTank

  override def fill(from: ForgeDirection, resource: FluidStack, doFill: Boolean) = tank.fill(resource, doFill)

  override def drain(from: ForgeDirection, resource: FluidStack, doDrain: Boolean): FluidStack = {
    if (resource == null || !resource.isFluidEqual(tank.getFluid)) null
    else tank.drain(resource.amount, doDrain)
  }

  override def drain(from: ForgeDirection, maxDrain: Int, doDrain: Boolean) = tank.drain(maxDrain, doDrain)

  override def getTankInfo(from: ForgeDirection): Array[FluidTankInfo] = Array(tank.getInfo)

  /**
    * If you change your tanks in serverUpdate, make sure to change them *BEFORE* calling super.serverUpdate().
    * This way the client will get notified of the change in the same tick.
    */
  override def serverUpdate(): Unit = {
    super.serverUpdate()
    if (!updateNeeded) return
    PacketHandler.INSTANCE.sendToDimension(new MessageFluidTankUpdate(xCoord, yCoord, zCoord, if (tank.getFluid == null) -1 else tank.getFluid.getFluidID, tank.getFluidAmount), getWorldObj.provider.dimensionId)
    updateNeeded = false
  }

  def setUpdateTank() = updateNeeded = true
}
