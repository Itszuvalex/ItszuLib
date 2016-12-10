package com.itszuvalex.itszulib.core.traits.tile

import com.itszuvalex.itszulib.api.core.Saveable
import com.itszuvalex.itszulib.core.TileEntityBase
import com.itszuvalex.itszulib.network.ItszuLibPacketHandler
import com.itszuvalex.itszulib.network.messages.MessageFluidTankUpdate
import net.minecraft.util.EnumFacing
import net.minecraftforge.fluids._
import net.minecraftforge.fluids.capability.IFluidHandler

/**
  * Created by Chris on 11/30/2014.
  */
trait TileFluidTank extends TileEntityBase with IFluidHandler {
  @Saveable var tank                  = defaultTank
            var updateNeeded: Boolean = false

  def defaultTank: FluidTank

  def fill(from: EnumFacing, resource: FluidStack, doFill: Boolean): Int = tank.fill(resource, doFill)

  def drain(from: EnumFacing, resource: FluidStack, doDrain: Boolean): FluidStack = {
    if (resource == null || !resource.isFluidEqual(tank.getFluid)) null
    else tank.drain(resource.amount, doDrain)
  }

  def drain(from: EnumFacing, maxDrain: Int, doDrain: Boolean): FluidStack = tank.drain(maxDrain, doDrain)

  def getTankInfo(from: EnumFacing): Array[FluidTankInfo] = Array(tank.getInfo)

  /**
    * If you change your tanks in serverUpdate, make sure to change them *BEFORE* calling super.serverUpdate().
    * This way the client will get notified of the change in the same tick.
    */
  override def serverUpdate(): Unit = {
    super.serverUpdate()
    if (!updateNeeded) return
    ItszuLibPacketHandler.INSTANCE.sendToDimension(new MessageFluidTankUpdate(getPos.getX, getPos.getY, getPos.getZ, if (tank.getFluid == null) null else FluidRegistry.getFluidName(tank.getFluid.getFluid), tank.getFluidAmount), getWorld.provider.getDimension)
    updateNeeded = false
  }

  def setUpdateTank() = updateNeeded = true
}
