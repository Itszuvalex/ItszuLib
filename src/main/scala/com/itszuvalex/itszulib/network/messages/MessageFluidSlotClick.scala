package com.itszuvalex.itszulib.network.messages

import com.itszuvalex.itszulib.core.traits.tile.{TileFluidTank, TileMultiFluidTank}
import com.itszuvalex.itszulib.network.PacketHandler
import cpw.mods.fml.common.network.simpleimpl.{IMessage, IMessageHandler, MessageContext}
import io.netty.buffer.ByteBuf
import net.minecraft.entity.player.EntityPlayerMP
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraftforge.fluids.{FluidContainerRegistry, IFluidContainerItem}

/**
  * Created by Alex on 11.10.2015.
  */
class MessageFluidSlotClick(var x: Int, var y: Int, var z: Int, var tankID: Int, var button: Int, var manualAccess: Int, var filterFluid: Int) extends IMessage with IMessageHandler[MessageFluidSlotClick, IMessage] {
  var tileSingleTank: TileFluidTank      = null
  var tileMultiTank : TileMultiFluidTank = null

  def this() = this(0, 0, 0, -1, -1, 0, -1)

  override def toBytes(buf: ByteBuf): Unit = {
    buf.writeInt(x)
    buf.writeShort(y)
    buf.writeInt(z)
    buf.writeInt(tankID)
    buf.writeByte(button)
    buf.writeByte(manualAccess)
    buf.writeInt(filterFluid)
  }

  override def fromBytes(buf: ByteBuf): Unit = {
    x = buf.readInt()
    y = buf.readShort()
    z = buf.readInt()
    tankID = buf.readInt()
    button = buf.readByte()
    manualAccess = buf.readByte()
    filterFluid = buf.readInt()
  }

  override def onMessage(message: MessageFluidSlotClick, ctx: MessageContext): IMessage = {
    val player = ctx.getServerHandler.playerEntity
    player.worldObj.getTileEntity(message.x, message.y, message.z) match {
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
    if (stk.stackSize == 1) {
      player.inventory.setItemStack(stack)
      val tag = new NBTTagCompound()
      if (stack != null) {
        stack.writeToNBT(tag)
      }
      PacketHandler.INSTANCE.sendTo(new MessageUpdateGuiItemStack(tag), player)
      return
    }
    stk.stackSize -= 1
    player.inventory.setItemStack(stk)
    val tag = new NBTTagCompound()
    stk.writeToNBT(tag)
    PacketHandler.INSTANCE.sendTo(new MessageUpdateGuiItemStack(tag), player)
    if (player.inventory.addItemStackToInventory(stack)) return
    player.inventory.player.dropPlayerItemWithRandomChoice(stack, false)
  }

  def handleClick(message: MessageFluidSlotClick, item: ItemStack): ItemStack = {
    if (item == null) return item
    if (!FluidContainerRegistry.isContainer(item)) return handleAltClick(message, item)
    message.tankID match {
      case -1 =>
        message.button match {
          case 0 =>
            if ((message.manualAccess & 1) == 0) return item
            if (!FluidContainerRegistry.isFilledContainer(item)) return item
            val itemFluid = FluidContainerRegistry.getFluidForFilledItem(item)
            if (itemFluid.getFluidID != message.filterFluid && message.filterFluid != -1) return item
            val amt = tileSingleTank.tank.fill(itemFluid, false)
            if (amt < itemFluid.amount) return item
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
            if (itemFluid.getFluidID != message.filterFluid && message.filterFluid != -1) return item
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
  }

  def handleAltClick(message: MessageFluidSlotClick, item: ItemStack): ItemStack = {
    if (!item.getItem.isInstanceOf[IFluidContainerItem]) return item
    val item2 = item.getItem.asInstanceOf[IFluidContainerItem]
    message.tankID match {
      case -1 =>
        message.button match {
          case 0 =>
            if ((message.manualAccess & 1) == 0) return item
            if (tileSingleTank.tank.getCapacity - tileSingleTank.tank.getFluidAmount == 0) return item
            if (item2.getFluid(item) == null) return item
            if (item2.getFluid(item).getFluidID != message.filterFluid && message.filterFluid != -1) return item
            tileSingleTank.tank.fill(item2.drain(item, tileSingleTank.tank.getCapacity - tileSingleTank.tank.getFluidAmount, true), true)
            tileSingleTank.setUpdateTank()
            item
          case 1 =>
            if ((message.manualAccess & 2) == 0) return item
            if (tileSingleTank.tank.getFluidAmount == 0) return item
            if (item2.getFluid(item).getFluid != tileSingleTank.tank.getFluid.getFluid) return item
            val amt = item2.fill(item, tileSingleTank.tank.getFluid, false)
            item2.fill(item, tileSingleTank.tank.drain(amt, true), true)
            tileSingleTank.setUpdateTank()
            item
        }
      case _ =>
        message.button match {
          case 0 =>
            if ((message.manualAccess & 1) == 0) return item
            if (tileMultiTank.tanks(message.tankID).getCapacity - tileMultiTank.tanks(message.tankID).getFluidAmount == 0) return item
            if (item2.getFluid(item) == null) return item
            if (item2.getFluid(item).getFluidID != message.filterFluid && message.filterFluid != -1) return item
            tileMultiTank.tanks(message.tankID).fill(item2.drain(item, tileMultiTank.tanks(message.tankID).getCapacity - tileMultiTank.tanks(message.tankID).getFluidAmount, true), true)
            tileMultiTank.setUpdateTanks()
            item
          case 1 =>
            if ((message.manualAccess & 2) == 0) return item
            if (tileMultiTank.tanks(message.tankID).getFluidAmount == 0) return item
            if (item2.getFluid(item).getFluid != tileMultiTank.tanks(message.tankID).getFluid.getFluid) return item
            val amt = item2.fill(item, tileMultiTank.tanks(message.tankID).getFluid, false)
            item2.fill(item, tileMultiTank.tanks(message.tankID).drain(amt, true), true)
            tileMultiTank.setUpdateTanks()
            item
        }
    }
  }
}
