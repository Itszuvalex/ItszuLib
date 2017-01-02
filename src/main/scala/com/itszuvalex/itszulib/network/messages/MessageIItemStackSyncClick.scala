package com.itszuvalex.itszulib.network.messages

import com.itszuvalex.itszulib.api.wrappers.{Converter, IItemStack}
import com.itszuvalex.itszulib.container.ContainerBase
import com.itszuvalex.itszulib.container.sync.{ISync, SyncItemStorageItemStack}
import com.itszuvalex.itszulib.network.ItszuLibPacketHandler
import com.itszuvalex.itszulib.util.Debug
import net.minecraft.entity.player.EntityPlayerMP
import net.minecraft.nbt.NBTTagCompound
import net.minecraftforge.fml.common.network.simpleimpl.{IMessage, MessageContext}

/**
  * Created by Chris on 12/13/2016.
  */
object MessageIItemStackSyncClick {
  val INDEX_KEY = "index"
  val CLICK_KEY = "click"

  sealed trait ClickType {def name: String}

  case object LEFT_CLICK extends ClickType {val name = "LeftClick"}

  case object RIGHT_CLICK extends ClickType {val name = "RightClick"}

  case object MIDDLE_CLICK extends ClickType {val name = "MiddleClick"}

  case object LEFT_RELEASE extends ClickType {val name = "LeftRelease"}

  case object RIGHT_RELEASE extends ClickType {val name = "RightRelease"}

  case object LEFT_DRAG extends ClickType {val name = "LeftDrag"}

  case object RIGHT_DRAG extends ClickType {val name = "RightDrag"}

  case object UNKNOWN extends ClickType {val name = "UnknownClick"}

  object ClickType {
    def fromName(string: String): ClickType = {
      string match {
        case LEFT_CLICK.name => LEFT_CLICK
        case RIGHT_CLICK.name => RIGHT_CLICK
        case MIDDLE_CLICK.name => MIDDLE_CLICK
        case LEFT_RELEASE.name => LEFT_RELEASE
        case RIGHT_RELEASE.name => RIGHT_RELEASE
        case LEFT_DRAG.name => LEFT_DRAG
        case RIGHT_DRAG.name => RIGHT_DRAG
        case UNKNOWN.name => UNKNOWN
        case _ => UNKNOWN
      }
    }
  }

}

class MessageIItemStackSyncClick(sync: ISync[_], click: MessageIItemStackSyncClick.ClickType) extends MessageUpdateNBT[MessageIItemStackSyncClick, IMessage]({
  val nbt = new NBTTagCompound
  nbt.setInteger(MessageIItemStackSyncClick.INDEX_KEY, Option(sync).map(_.syncIndex).getOrElse(0))
  nbt.setString(MessageIItemStackSyncClick.CLICK_KEY, click.name)
  nbt
}) {

  def this() = this(null, MessageIItemStackSyncClick.LEFT_CLICK)

  override def onMessage(message: MessageIItemStackSyncClick, ctx: MessageContext): IMessage = {
    ctx.getServerHandler.playerEntity.openContainer match {
      case a: ContainerBase =>
        val sync = a.getSync(message.nbt.getInteger(MessageIItemStackSyncClick.INDEX_KEY))
        val click = MessageIItemStackSyncClick.ClickType.fromName(message.nbt.getString(MessageIItemStackSyncClick.CLICK_KEY))
        click match {
          case MessageIItemStackSyncClick.LEFT_CLICK => handleLeftClick(a, a.getSync(message.nbt.getInteger(MessageIItemStackSyncClick.INDEX_KEY)).asInstanceOf[SyncItemStorageItemStack], ctx.getServerHandler.playerEntity)
          case MessageIItemStackSyncClick.RIGHT_CLICK => handleRightClick(a, a.getSync(message.nbt.getInteger(MessageIItemStackSyncClick.INDEX_KEY)).asInstanceOf[SyncItemStorageItemStack], ctx.getServerHandler.playerEntity)
          case _ =>
        }
      case _ =>
    }
    null
  }

  def handleLeftClick(container: ContainerBase, sync: SyncItemStorageItemStack, player: EntityPlayerMP): Unit = {
    (Converter.IItemStackFromItemStack(player.inventory.getItemStack), sync.storage(sync.storageIndex)) match {
      case (null, null) =>
      case (a, b) if a.isEmpty && b.isEmpty => // Do nothing
      case (a, b) if a.isEmpty && !b.isEmpty => // Set inventory item from slot.
        player.inventory.setItemStack(b.toMinecraft)
        sync.storage(sync.storageIndex) = IItemStack.Empty
        player.inventory.markDirty()
        ItszuLibPacketHandler.INSTANCE.sendTo(new MessageUpdatePlayerInventory(player.inventory.getItemStack.serializeNBT()), player)
      case (a, b) if !a.isEmpty && b.isEmpty =>
        player.inventory.setItemStack(sync.storage.insert(sync.storageIndex, Converter.IItemStackFromItemStack(player.inventory.getItemStack)).toMinecraft)
        player.inventory.markDirty()
        ItszuLibPacketHandler.INSTANCE.sendTo(new MessageUpdatePlayerInventory(player.inventory.getItemStack.serializeNBT()), player)
      case (a, b) if !a.isEmpty && !b.isEmpty =>
        if (IItemStack.itemStackEquality.apply(a, b)) {
          player.inventory.setItemStack(sync.storage.insert(sync.storageIndex, Converter.IItemStackFromItemStack(player.inventory.getItemStack)).toMinecraft)
        }
        else {
          player.inventory.setItemStack(b.toMinecraft)
          sync.storage(sync.storageIndex) = a
        }
        player.inventory.markDirty()
        ItszuLibPacketHandler.INSTANCE.sendTo(new MessageUpdatePlayerInventory(player.inventory.getItemStack.serializeNBT()), player)
      case _ => Debug.assert(false, "Case not matched on HandleLeftClick")
    }
  }

  def handleRightClick(container: ContainerBase, sync: SyncItemStorageItemStack, player: EntityPlayerMP): Unit = {
    (Converter.IItemStackFromItemStack(player.inventory.getItemStack), sync.storage(sync.storageIndex)) match {
      case (null, null) =>
      case (a, b) if a.isEmpty && b.isEmpty => // Do nothing
      case (a, b) if a.isEmpty && !b.isEmpty => // Set inventory item from slot.
        player.inventory.setItemStack(sync.storage.split(sync.storageIndex, Math.ceil(sync.storage(sync.storageIndex).stackSize / 2f).toInt).toMinecraft)
        player.inventory.markDirty()
        ItszuLibPacketHandler.INSTANCE.sendTo(new MessageUpdatePlayerInventory(player.inventory.getItemStack.serializeNBT()), player)
      case (a, b) if !a.isEmpty && b.isEmpty =>
        val copy = a.copy()
        copy.stackSize = 1
        if (sync.storage.insert(sync.storageIndex, copy).isEmpty) {
          a.stackSize -= 1
          player.inventory.markDirty()
          ItszuLibPacketHandler.INSTANCE.sendTo(new MessageUpdatePlayerInventory(player.inventory.getItemStack.serializeNBT()), player)
        }
      case (a, b) if !a.isEmpty && !b.isEmpty =>
        if (IItemStack.itemStackEquality.apply(a, b)) {
          val copy = a.copy()
          copy.stackSize = 1
          if (sync.storage.insert(sync.storageIndex, copy).isEmpty) {
            a.stackSize -= 1
            player.inventory.markDirty()
            ItszuLibPacketHandler.INSTANCE.sendTo(new MessageUpdatePlayerInventory(player.inventory.getItemStack.serializeNBT()), player)
          }
        }
      case _ => Debug.assert(false, "Case not matched on HandleLeftClick")
    }
  }
}
