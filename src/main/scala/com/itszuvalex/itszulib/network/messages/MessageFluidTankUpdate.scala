package com.itszuvalex.itszulib.network.messages

import com.itszuvalex.itszulib.core.traits.tile.{TileFluidTank, TileMultiFluidTank}
import com.itszuvalex.itszulib.implicits.SerializationImplicits._
import io.netty.buffer.ByteBuf
import net.minecraft.client.Minecraft
import net.minecraft.util.math.BlockPos
import net.minecraftforge.fluids.{FluidRegistry, FluidStack}
import net.minecraftforge.fml.common.network.simpleimpl.{IMessage, MessageContext}

/**
  * Created by Alex on 11.10.2015.
  */
class MessageFluidTankUpdate(var x: Int, var y: Int, var z: Int, var tankID: Int, var fluidName: String, var amount: Int) extends MessageBase[MessageFluidTankUpdate, IMessage] {
  def this() = this(0, 0, 0, -1, null, -1)

  def this(_x: Int, _y: Int, _z: Int, fID: String, amt: Int) = this(_x, _y, _z, -1, fID, amt)

  override def toBytes(buf: ByteBuf): Unit = {
    buf.writeInt(x)
    buf.writeShort(y)
    buf.writeInt(z)
    buf.writeInt(tankID)
    buf.writeString(fluidName)
    buf.writeInt(amount)
  }

  override def fromBytes(buf: ByteBuf): Unit = {
    x = buf.readInt()
    y = buf.readShort()
    z = buf.readInt()
    tankID = buf.readInt()
    fluidName = buf.readString()
    amount = buf.readInt()
  }

  override def onMessage(message: MessageFluidTankUpdate, ctx: MessageContext): IMessage = {
    val world = Minecraft.getMinecraft.world
    world.getTileEntity(new BlockPos(message.x, message.y, message.z)) match {
      case tank: TileFluidTank =>
        tank.tank.setFluid(if (message.fluidName == null || message.fluidName.isEmpty) null else new FluidStack(FluidRegistry.getFluid(message.fluidName), message.amount))
      case tank: TileMultiFluidTank =>
        tank.tanks(message.tankID).setFluid(if (message.fluidName == null || message.fluidName.isEmpty) null else new FluidStack(FluidRegistry.getFluid(message.fluidName), message.amount))
      case _ =>
    }
    null
  }
}
