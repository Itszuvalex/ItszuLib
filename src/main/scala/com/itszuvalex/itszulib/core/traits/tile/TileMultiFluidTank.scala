package com.itszuvalex.itszulib.core.traits.tile

import com.itszuvalex.itszulib.core.TileEntityBase
import com.itszuvalex.itszulib.network.ItszuLibPacketHandler
import com.itszuvalex.itszulib.network.messages.MessageFluidTankUpdate
import net.minecraft.util.EnumFacing
import net.minecraftforge.fluids._
import net.minecraftforge.fluids.capability.IFluidHandler

/**
  * Created by Alex on 04.10.2015.
  */
trait TileMultiFluidTank extends TileEntityBase with IFluidHandler {
  var tanks: Array[FluidTank] = defaultTanks

  var updateNeeded: Boolean = false

  def defaultTanks: Array[FluidTank]

  override def fill(resource: FluidStack, doFill: Boolean): Int

  def fill(id: Int, from: EnumFacing, resource: FluidStack, doFill: Boolean) = tanks(id).fill(resource, doFill)

  override def drain(resource: FluidStack, doDrain: Boolean): FluidStack

  def drain(id: Int, from: EnumFacing, resource: FluidStack, doDrain: Boolean): FluidStack = {
    if (resource == null || !resource.isFluidEqual(tanks(id).getFluid)) null
    else tanks(id).drain(resource.amount, doDrain)
  }

  /**
    * If you change your tanks in serverUpdate, make sure to change them *BEFORE* calling super.serverUpdate().
    * This way the client will get notified of the change in the same tick.
    */
  override def serverUpdate(): Unit = {
    super.serverUpdate()
    if (!updateNeeded) return
    tanks.indices.foreach { i =>
      val tank = tanks(i)
      ItszuLibPacketHandler.INSTANCE.sendToDimension(new MessageFluidTankUpdate(getPos.getX, getPos.getY, getPos.getZ, i, if (tank.getFluid == null) null else FluidRegistry.getFluidName(tank.getFluid.getFluid), tank.getFluidAmount), getWorld.provider.getDimension)
    }
    updateNeeded = false
  }

  def setUpdateTanks() = updateNeeded = true
}
