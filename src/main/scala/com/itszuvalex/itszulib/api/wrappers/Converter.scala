package com.itszuvalex.itszulib.api.wrappers

import com.itszuvalex.itszulib.api.core.IModuleProvider
import com.itszuvalex.itszulib.api.storage._
import net.minecraft.block.Block
import net.minecraft.inventory.{IInventory, ISidedInventory}
import net.minecraft.item.{Item, ItemStack}
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.EnumFacing
import net.minecraft.world.World
import net.minecraftforge.common.capabilities.ICapabilityProvider
import net.minecraftforge.fluids.FluidStack
import net.minecraftforge.fluids.capability.{IFluidHandler, IFluidTankProperties}
import net.minecraftforge.items.{IItemHandler, IItemHandlerModifiable}

object Converter {
  def IWorldFromWorld(world: World): IWorld = world match {
    case null => null
    case w: IWorld => w
    case _ => new WrapperWorld(world)
  }

  def WorldFromIWorld(world: IWorld): World = world match {
    case null => null
    case w: World => w
    case _ => world.toMinecraft
  }

  def IBlockFromBlock(block: Block): IBlock = block match {
    case null => null
    case b: IBlock => b
    case _ => new WrapperBlock(block)
  }

  def BlockFromIBlock(block: IBlock): Block = block match {
    case null => null
    case b: Block => b
    case _ => block.toMinecraft
  }

  def IItemFromItem(item: Item): IItem = item match {
    case null => null
    case i: IItem => i
    case _ => new WrapperItem(item)
  }

  def ItemFromIItem(item: IItem): Item = item match {
    case null => null
    case i: Item => i
    case _ => item.toMinecraft
  }

  def IModuleProviderFromICapabilityProvider(i: ICapabilityProvider): IModuleProvider = i match {
    case null => null
    case mp: IModuleProvider => mp
    case _ => new WrapperCapabilityProvider(i)
  }

  def IInventoryFromIItemStorage(storage: IItemStorage): IInventory = storage match {
    case null => null
    case i: IInventory => i
    case _ => new WrapperIItemStorage(storage)
  }

  def IItemStorageFromIInventory(inventory: IInventory): IItemStorage = inventory match {
    case null => null
    case s: IItemStorage => s
    case _ => new ItemStorageInventory(inventory)
  }

  def IItemStorageFromISidedInventory(iSidedInventory: ISidedInventory, facing: EnumFacing): IItemStorage = new ItemStorageSidedInventory(iSidedInventory, facing)

  def IItemStackFromItemStack(item: ItemStack): IItemStack = item match {
    case null => null
    case _ => WrapperVanillaItemStack(item)
  }

  def ItemStackFromIItemStack(item: IItemStack): ItemStack = item match {
    case null => null
    case _ => item.toMinecraft
  }

  def IFluidStackFromFluidStack(fluid: FluidStack): IFluidStack = WrapperVanillaFluidStack(fluid)

  def FluidStackFromIFluidStack(fluid: IFluidStack): FluidStack = fluid.toMinecraft

  def IItemStorageFromIItemHandler(handler: IItemHandler): IItemStorage = handler match {
    case null => null
    case s: IItemStorage => s
    case _ => new ItemStorageItemHandler(handler)
  }

  def IItemHandlerModifiableFromIItemStorage(storage: IItemStorage): IItemHandlerModifiable = storage match {
    case null => null
    case h: IItemHandlerModifiable => h
    case _ => new WrapperItemHandlerModifiable(storage)
  }

  def IFluidStorageFromIFluidHandler(handler: IFluidHandler): IFluidStorage = handler match {
    case null => null
    case t: IFluidStorage => t
    case _ => new FluidStorageFluidHandler(handler)
  }

  def IFluidHandlerFromIFluidStorage(storage: IFluidStorage): IFluidHandler = storage match {
    case null => null
    case t: IFluidHandler => t
    case _ => new WrapperFluidStorageIFluidHandler(storage)
  }

  def IFluidTankPropertiesFromIFluidStorage(storage: IFluidStorage): Array[IFluidTankProperties] = storage.indices.map(new WrapperFluidTankProperties(storage, _)).toArray

  def ITileEntityFromTileEntity(te: TileEntity): ITileEntity = te match {
    case null => null
    case ite: ITileEntity => ite
    case _ => new WrapperTileEntity(te)
  }

  def TileEntityFromITileEntity(ite: ITileEntity): TileEntity = ite match {
    case null => null
    case te: TileEntity => te
    case _ => ite.toMinecraft
  }

}
