package com.itszuvalex.itszulib.network.messages

import com.itszuvalex.itszulib.core.traits.tile.{TileFluidTank, TileMultiFluidTank}
import cpw.mods.fml.common.network.simpleimpl.{IMessage, IMessageHandler, MessageContext}
import io.netty.buffer.ByteBuf
import net.minecraft.client.Minecraft
import net.minecraftforge.fluids.{FluidRegistry, FluidStack}

/**
  * Created by Alex on 11.10.2015.
  */
class MessageFluidTankUpdate(var x: Int, var y: Int, var z: Int, var tankID: Int, var fluidID: Int, var amount: Int) extends IMessage with IMessageHandler[MessageFluidTankUpdate, IMessage] {
  def this() = this(0, 0, 0, -1, -1, -1)

  def this(_x: Int, _y: Int, _z: Int, fID: Int, amt: Int) = this(_x, _y, _z, -1, fID, amt)

  override def toBytes(buf: ByteBuf): Unit = {
    buf.writeInt(x)
    buf.writeShort(y)
    buf.writeInt(z)
    buf.writeInt(tankID)
    buf.writeInt(fluidID)
    buf.writeInt(amount)
  }

  override def fromBytes(buf: ByteBuf): Unit = {
    x = buf.readInt()
    y = buf.readShort()
    z = buf.readInt()
    tankID = buf.readInt()
    fluidID = buf.readInt()
    amount = buf.readInt()
  }

  override def onMessage(message: MessageFluidTankUpdate, ctx: MessageContext): IMessage = {
    val world = Minecraft.getMinecraft.theWorld
    world.getTileEntity(message.x, message.y, message.z) match {
      case tank: TileFluidTank =>
        tank.tank.setFluid(if (message.fluidID == -1) null else new FluidStack(FluidRegistry.getFluid(message.fluidID), message.amount))
      case tank: TileMultiFluidTank =>
        tank.tanks(message.tankID).setFluid(if (message.fluidID == -1) null else new FluidStack(FluidRegistry.getFluid(message.fluidID), message.amount))
      case _ =>
    }
    null
  }
}
