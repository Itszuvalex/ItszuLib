package com.itszuvalex.itszulib.api.wrappers

import com.itszuvalex.itszulib.api.storage._
import net.minecraft.inventory.{IInventory, ISidedInventory}
import net.minecraft.item.ItemStack
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.EnumFacing
import net.minecraftforge.fluids.FluidStack
import net.minecraftforge.fluids.capability.IFluidHandler
import net.minecraftforge.items.{IItemHandler, IItemHandlerModifiable}

/**
  * Created by Chris on 7/31/2016.
  */
object Converter {
  def IInventoryFromIItemStorage(storage: IItemStorage): IInventory = new WrapperIItemStorage(storage)

  def IItemStorageFromIInventory(inventory: IInventory): IItemStorage = new ItemStorageInventory(inventory)

  def IItemStorageFromISidedInventory(iSidedInventory: ISidedInventory, facing: EnumFacing): IItemStorage = new ItemStorageSidedInventory(iSidedInventory, facing)

  def IItemStackFromItemStack(item: ItemStack): IItemStack = WrapperVanillaItemStack(item)

  def ItemStackFromIItemStack(item: IItemStack): ItemStack = item.toMinecraft

  def IFluidStackFromFluidStack(fluid: FluidStack): IFluidStack = WrapperVanillaFluidStack(fluid)

  def FluidStackFromIFluidStack(fluid: IFluidStack): FluidStack = fluid.toMinecraft

  def IItemStorageFromIItemHandler(handler: IItemHandler): IItemStorage = new ItemStorageItemHandler(handler)

  def IItemHandlerModifiableFromIItemStorage(storage: IItemStorage): IItemHandlerModifiable = new WrapperItemHandlerModifiable(storage)

  def IFluidStorageFromIFluidHandler(handler: IFluidHandler): IFluidStorage = new WrapperFluidStorageHandler(handler)

  def IFluidHandlerFromIFluidStorage(storage: IFluidStorage): IFluidHandler = storage

  def ITileEntityFromTileEntity(te: TileEntity): ITileEntity = te match {
    case null => null
    case ite: ITileEntity => ite
    case te: TileEntity => new WrapperTileEntity(te)
    case _ => null
  }

  def TileEntityFromITileEntity(ite: ITileEntity): TileEntity = ite.toMinecraft

}
