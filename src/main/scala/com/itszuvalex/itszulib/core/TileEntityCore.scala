package com.itszuvalex.itszulib.core

import com.itszuvalex.itszulib.api.core.{BlockIdentifier, IModule, Loc4}
import com.itszuvalex.itszulib.api.wrappers._
import net.minecraft.block.state.IBlockState
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.network.NetworkManager
import net.minecraft.network.play.server.SPacketUpdateTileEntity
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.math.BlockPos
import net.minecraft.util.{EnumFacing, EnumHand}
import net.minecraftforge.common.capabilities.Capability

import scala.collection.mutable

abstract class TileEntityCore extends TileEntity with ITileEntity with IBlockCallbacks {
  val moduleCapabilityMap = new ModuleCapabilityMap
  val modules             = new mutable.ArrayBuffer[ITileEntityModule[_]]()

  def addTileEntityModule[T](module: ITileEntityModule[T]): Unit = {
    modules += module
    if (module.module != null)
      moduleCapabilityMap.addModule(module.module, module.faceToModuleMapper(this))
  }

  override def toMinecraft: TileEntity = this

  override def hasModule(mod: IModule[_], facing: EnumFacing): Boolean = moduleCapabilityMap.hasModule(mod, facing)

  override def getModule[T](mod: IModule[T], facing: EnumFacing): T = moduleCapabilityMap.getModule(mod, facing)

  override def hasCapability(capability: Capability[_], facing: EnumFacing): Boolean = {
    if (moduleCapabilityMap.hasCapability(capability, facing))
      true
    else
      super.hasCapability(capability, facing)
  }

  override def getCapability[T](capability: Capability[T], facing: EnumFacing): T = {
    moduleCapabilityMap.capabilityOption(capability, facing).getOrElse(super.getCapability(capability, facing))
  }

  override def getUpdatePacket: SPacketUpdateTileEntity = {
    if (!hasDescription) null
    else new SPacketUpdateTileEntity(getPos, getBlockType.getMetaFromState(getWorld.getBlockState(getPos)), getUpdateTag)
  }

  override def getUpdateTag: NBTTagCompound = {
    val compound = super.getUpdateTag
    saveToDescriptionCompound(compound)
    compound
  }

  def saveToDescriptionCompound(compound: NBTTagCompound): Unit = {
    modules.view.filter(_.hasDescriptionNBT).foreach { x =>
      val c = new NBTTagCompound
      x.writeDescriptionNBT(c)
      compound.setTag(x.name, c)
    }
  }

  def hasDescription: Boolean = modules.exists(_.hasDescriptionNBT)

  override def onDataPacket(net: NetworkManager, pkt: SPacketUpdateTileEntity) {
    super.onDataPacket(net, pkt)
    handleDescriptionNBT(pkt.getNbtCompound)
  }

  override def handleUpdateTag(tag: NBTTagCompound): Unit = {
    super.readFromNBT(tag) // Get BlockPos, ForgeData, and Serializable Forge Capabilities
    handleDescriptionNBT(tag)
  }

  def handleDescriptionNBT(compound: NBTTagCompound) {
    modules.view.filter(_.hasDescriptionNBT).foreach { m =>
      m.readDescriptionNBT(compound.getCompoundTag(m.name))
    }
  }

  def getLoc = new Loc4(this.asInstanceOf[ITileEntity])

  def loadInfoFromItemNBT(compound: NBTTagCompound): Unit = {
    modules.view.filter(_.hasItemNBT).foreach { x =>
      val c = new NBTTagCompound
      x.writeItemNBT(c)
      compound.setTag(x.name, c)
    }
  }

  def saveInfoToItemNBT(compound: NBTTagCompound) {
    modules.view.filter(_.hasItemNBT).foreach { m =>
      m.readItemNBT(compound.getCompoundTag(m.name))
    }
  }

  override def onSideActivate(par5EntityPlayer: EntityPlayer, side: EnumFacing): Boolean = {
    if (hasGUI) {
      par5EntityPlayer.openGui(getMod, getGuiID, getWorld, getPos.getX, getPos.getY, getPos.getZ)
      return true
    }
    false
  }

  def hasGUI = false

  /**
    * @return GuiID, if GUI handler uses ids and not checking instanceof
    */
  def getGuiID: Int = -1

  def getMod: AnyRef

  def canPlayerUse(player: EntityPlayer): Boolean = hasGUI


  override def onBlockBreak(state: IBlockState): Unit = {
    modules.view.foreach(_.onBlockBreak(this, state))
  }

  override def onBlockPlacedBy(iworld: IWorld, pos: BlockPos, state: IBlockState, placer: EntityLivingBase, istack: IItemStack): Unit = {
    modules.view.foreach(_.onBlockPlacedBy(iworld, pos, state, placer, istack))
  }


  override def getIWorld: IWorld = Converter.IWorldFromWorld(getWorld)

  override def hasIWorld: Boolean = hasWorld

  override def readFromNBT(compound: NBTTagCompound): Unit = {
    modules.view.filter(_.hasWorldNBT).foreach { m =>
      m.readWorldNBT(compound.getCompoundTag(m.name))
    }
    super.readFromNBT(compound)
  }

  override def writeToNBT(compound: NBTTagCompound): NBTTagCompound = {
    modules.view.filter(_.hasWorldNBT).foreach { x =>
      val c = new NBTTagCompound
      x.writeWorldNBT(c)
      compound.setTag(x.name, c)
    }
    super.writeToNBT(compound)
  }

  override def markDirtyForSave(): Unit = markDirty()

  override def getBlockIdentifier: BlockIdentifier = BlockIdentifier(getBlockType)

  override def invalidate(): Unit = {
    modules.view.foreach(_.invalidate(this))
    super.invalidate()
  }

  override def validate(): Unit = {
    modules.view.foreach(_.validate(this))
    super.validate()
  }

  override def onChunkUnload(): Unit = {
    modules.view.foreach(_.onChunkUnload(this))
    super.onChunkUnload()
  }

  /**
    * Use this to do things on if(firstTick)
    */
  override def onLoad(): Unit = {
    modules.view.foreach(_.onLoad(this))
    super.onLoad()
  }

  override def onBlockActivated(world: IWorld, pos: BlockPos, state: IBlockState, playerIn: EntityPlayer, hand: EnumHand, facing: EnumFacing, hitX: Float, hitY: Float, hitZ: Float): Boolean = {
    modules.view.exists(_.onBlockActivated(world, pos, state, playerIn, hand, facing, hitX, hitY, hitZ))
  }
}
