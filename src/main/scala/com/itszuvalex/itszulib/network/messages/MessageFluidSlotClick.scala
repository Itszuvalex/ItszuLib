package com.itszuvalex.itszulib.network.messages

import com.itszuvalex.itszulib.api.core.Loc4
import com.itszuvalex.itszulib.core.traits.tile.{TileFluidTank, TileMultiFluidTank}
import com.itszuvalex.itszulib.implicits.SerializationImplicits._
import com.itszuvalex.itszulib.network.ItszuLibPacketHandler
import io.netty.buffer.ByteBuf
import net.minecraft.entity.player.EntityPlayerMP
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraftforge.fluids.FluidTank
import net.minecraftforge.fluids.capability.{CapabilityFluidHandler, IFluidHandler}
import net.minecraftforge.fml.common.network.simpleimpl.{IMessage, MessageContext}

/**
  * Created by Alex on 11.10.2015.
  */
class MessageFluidSlotClick(var loc: Loc4, var tankID: Int, var button: Int, var manualAccess: Int, var filterFluid: String) extends MessageBase[MessageFluidSlotClick, IMessage] {
  var tileSingleTank: TileFluidTank      = _
  var tileMultiTank : TileMultiFluidTank = _

  def this() = this(Loc4(0, 0, 0, 0), -1, -1, 0, null)

  override def toBytes(buf: ByteBuf): Unit = {
    buf.writeInt(loc.x)
    buf.writeShort(loc.y)
    buf.writeInt(loc.z)
    buf.writeInt(loc.dim)
    buf.writeInt(tankID)
    buf.writeByte(button)
    buf.writeByte(manualAccess)
    buf.writeString(filterFluid)
  }

  override def fromBytes(buf: ByteBuf): Unit = {
    loc.x = buf.readInt()
    loc.y = buf.readShort()
    loc.z = buf.readInt()
    loc.dim = buf.readInt()
    tankID = buf.readInt()
    button = buf.readByte()
    manualAccess = buf.readByte()
    filterFluid = buf.readString()
  }

  override def onMessage(message: MessageFluidSlotClick, ctx: MessageContext): IMessage = {
    val player = ctx.getServerHandler.player
    player.world.getTileEntity(message.loc.getPos) match {
      case tank: TileFluidTank => tileSingleTank = tank; tankID = -1
      case tank: TileMultiFluidTank => tileMultiTank = tank
      case _ => return null
    }
    val stack = handleClick(message, player.inventory.getItemStack)
    if (ItemStack.areItemStacksEqual(stack, player.inventory.getItemStack)) return null
    handleItemStackUpdate(player, stack)
    null
  }

  def handleItemStackUpdate(player: EntityPlayerMP, stack: ItemStack): Unit = {
    val stk = player.inventory.getItemStack
    if (stk.getCount == 1) {
      player.inventory.setItemStack(stack)
      val tag = new NBTTagCompound()
      if (stack != null) {
        stack.writeToNBT(tag)
      }
      ItszuLibPacketHandler.INSTANCE.sendTo(new MessageUpdatePlayerInventory(tag), player)
      return
    }
    stk.setCount(stk.getCount - 1)
    player.inventory.setItemStack(stk)
    val tag = new NBTTagCompound()
    stk.writeToNBT(tag)
    ItszuLibPacketHandler.INSTANCE.sendTo(new MessageUpdatePlayerInventory(tag), player)
    if (player.inventory.addItemStackToInventory(stack)) return
    player.inventory.player.dropItem(stack, false)
  }

  /*def handleClick(message: MessageFluidSlotClick, item: ItemStack): ItemStack = {
    if (item == null) return item
    if (!item.hasCapability(CapabilityFluidHandler.FLUID_HANDLER_ITEM_CAPABILITY, null)) return handleAltClick(message, item)
    val cap = item.getCapability[IFluidHandlerItem](CapabilityFluidHandler.FLUID_HANDLER_ITEM_CAPABILITY, null)
    val contents = cap.drain(Int.MaxValue, false)
    val simple = cap.isInstanceOf[FluidHandlerItemStackSimple]
    message.tankID match {
      case -1 =>
        message.button match {
          case 0 =>
            if ((message.manualAccess & 1) == 0) return item
            if (contents == null || contents.amount == 0) return item
            if (FluidRegistry.getFluidName(contents) != message.filterFluid && message.filterFluid != null) return item
            val amt = tileSingleTank.tank.fill(contents, false)
            if (amt < contents.amount && simple) return item
            tileSingleTank.tank.fill(itemFluid, true)
            tileSingleTank.setUpdateTank()
            FluidContainerRegistry.drainFluidContainer(item)
          case 1 =>
            if ((message.manualAccess & 2) == 0) return item
            if (!FluidContainerRegistry.isEmptyContainer(item)) return item
            val itemFluidCap = FluidContainerRegistry.getContainerCapacity(tileSingleTank.tank.getFluid, item)
            if (itemFluidCap == 0) return item
            if (tileSingleTank.tank.getFluidAmount < itemFluidCap) return item
            tileSingleTank.setUpdateTank()
            FluidContainerRegistry.fillFluidContainer(tileSingleTank.tank.drain(itemFluidCap, true), item)
        }
      case _ =>
        message.button match {
          case 0 =>
            if ((message.manualAccess & 1) == 0) return item
            if (!FluidContainerRegistry.isFilledContainer(item)) return item
            val itemFluid = FluidContainerRegistry.getFluidForFilledItem(item)
            if (itemFluid.getFluid.getID != message.filterFluid && message.filterFluid != -1) return item
            val amt = tileMultiTank.tanks(message.tankID).fill(itemFluid, false)
            if (amt < itemFluid.amount) return item
            tileMultiTank.tanks(message.tankID).fill(itemFluid, true)
            tileMultiTank.setUpdateTanks()
            FluidContainerRegistry.drainFluidContainer(item)
          case 1 =>
            if ((message.manualAccess & 2) == 0) return item
            if (!FluidContainerRegistry.isEmptyContainer(item)) return item
            val itemFluidCap = FluidContainerRegistry.getContainerCapacity(tileMultiTank.tanks(message.tankID).getFluid, item)
            if (itemFluidCap == 0) return item
            if (tileMultiTank.tanks(message.tankID).getFluidAmount < itemFluidCap) return item
            tileMultiTank.setUpdateTanks()
            FluidContainerRegistry.fillFluidContainer(tileMultiTank.tanks(message.tankID).drain(itemFluidCap, true), item)
        }
      }
  }*/

  private def FillTankFromHandler(tank: FluidTank, fluidHandler: IFluidHandler): Unit = {
    val room = tank.getCapacity - tank.getFluidAmount
    if (!fluidHandler.getTankProperties()(0).canDrain || !tank.canFill) return
    val fl = fluidHandler.drain(room, false)
    if (!tank.canFillFluidType(fl) || (tank.getFluid != null && !tank.getFluid.isFluidEqual(fl))) return
    tank.fill(fluidHandler.drain(room, true), true)
  }

  private def DrainTankIntoHandler(tank: FluidTank, fluidHandler: IFluidHandler): Unit = {
    if (tank.getFluidAmount == 0) return
    val prop = fluidHandler.getTankProperties()(0)
    if (!prop.canFill || !tank.canDrain) return
    val wouldRet = tank.drain(tank.getFluidAmount, false)
    if (!prop.canFillFluidType(wouldRet) || (prop.getContents != null && !prop.getContents.isFluidEqual(wouldRet))) return
    val wouldFill = fluidHandler.fill(wouldRet, false)
    fluidHandler.fill(tank.drain(wouldFill, true), true)
  }

  def handleClick(message: MessageFluidSlotClick, item: ItemStack): ItemStack = {
    if (!item.hasCapability(CapabilityFluidHandler.FLUID_HANDLER_ITEM_CAPABILITY, null)) return item
    val fluidHandler = item.getCapability(CapabilityFluidHandler.FLUID_HANDLER_ITEM_CAPABILITY, null)
    message.tankID match {
      case -1 =>
        message.button match {
          case 0 =>
            if ((message.manualAccess & 1) == 0) return item
//            FillTankFromHandler(tileSingleTank.tank, fluidHandler)
//            tileSingleTank.setUpdateTank()
            fluidHandler.getContainer
          case 1 =>
            if ((message.manualAccess & 2) == 0) return item
//            DrainTankIntoHandler(tileSingleTank.tank, fluidHandler)
//            tileSingleTank.setUpdateTank()
            fluidHandler.getContainer
        }
      case _ =>
        message.button match {
          case 0 =>
            if ((message.manualAccess & 1) == 0) return item
            FillTankFromHandler(tileMultiTank.tanks(message.tankID), fluidHandler)
            tileMultiTank.setUpdateTanks()
            fluidHandler.getContainer
          case 1 =>
            if ((message.manualAccess & 2) == 0) return item
            DrainTankIntoHandler(tileMultiTank.tanks(message.tankID), fluidHandler)
            tileMultiTank.setUpdateTanks()
            fluidHandler.getContainer
        }
    }
  }
}
