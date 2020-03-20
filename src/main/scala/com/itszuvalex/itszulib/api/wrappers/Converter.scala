package com.itszuvalex.itszulib.api.wrappers

import com.itszuvalex.itszulib.api.storage._
import net.minecraft.inventory.{IInventory, ISidedInventory}
import net.minecraft.item.ItemStack
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.EnumFacing
import net.minecraftforge.fluids.FluidStack
import net.minecraftforge.fluids.capability.{IFluidHandler, IFluidTankProperties}
import net.minecraftforge.items.{IItemHandler, IItemHandlerModifiable}

/**
  * Created by Chris on 7/31/2016.
  */
object Converter {
  def IInventoryFromIItemStorage(storage: IItemStorage): IInventory = storage match {
    case i: IInventory => i
    case _ => new WrapperIItemStorage(storage)
  }

  def IItemStorageFromIInventory(inventory: IInventory): IItemStorage = inventory match {
    case s: IItemStorage => s
    case _ => new ItemStorageInventory(inventory)
  }

  def IItemStorageFromISidedInventory(iSidedInventory: ISidedInventory, facing: EnumFacing): IItemStorage = new ItemStorageSidedInventory(iSidedInventory, facing)

  def IItemStackFromItemStack(item: ItemStack): IItemStack = WrapperVanillaItemStack(item)

  def ItemStackFromIItemStack(item: IItemStack): ItemStack = item.toMinecraft

  def IFluidStackFromFluidStack(fluid: FluidStack): IFluidStack = WrapperVanillaFluidStack(fluid)

  def FluidStackFromIFluidStack(fluid: IFluidStack): FluidStack = fluid.toMinecraft

  def IItemStorageFromIItemHandler(handler: IItemHandler): IItemStorage = handler match {
    case s: IItemStorage => s
    case _ => new ItemStorageItemHandler(handler)
  }

  def IItemHandlerModifiableFromIItemStorage(storage: IItemStorage): IItemHandlerModifiable = storage match {
    case h: IItemHandlerModifiable => h
    case _ => new WrapperItemHandlerModifiable(storage)
  }

  def IFluidStorageFromIFluidHandler(handler: IFluidHandler): IFluidStorage = handler match {
    case t: IFluidStorage => t
    case _ => new FluidStorageFluidHandler(handler)
  }

  def IFluidHandlerFromIFluidStorage(storage: IFluidStorage): IFluidHandler = storage match {
    case t: IFluidHandler => t
    case _ => new WrapperFluidStorageIFluidHandler(storage)
  }

  def IFluidTankPropertiesFromIFluidStorage(storage: IFluidStorage): Array[IFluidTankProperties] = storage.indices.map(new WrapperFluidTankProperties(storage, _)).toArray

  def ITileEntityFromTileEntity(te: TileEntity): ITileEntity = te match {
    case null => null
    case ite: ITileEntity => ite
    case te: TileEntity => new WrapperTileEntity(te)
    case _ => null
  }

  def TileEntityFromITileEntity(ite: ITileEntity): TileEntity = ite match {
    case te: TileEntity => te
    case _ => ite.toMinecraft
  }

}
