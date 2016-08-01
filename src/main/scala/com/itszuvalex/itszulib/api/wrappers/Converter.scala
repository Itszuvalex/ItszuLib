package com.itszuvalex.itszulib.api.wrappers

import com.itszuvalex.itszulib.api.storage.{IItemStorage, ItemStorageInventory, ItemStorageItemHandler, ItemStorageSidedInventory}
import net.minecraft.inventory.{IInventory, ISidedInventory}
import net.minecraft.item.ItemStack
import net.minecraft.util.EnumFacing
import net.minecraftforge.fluids.FluidStack
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

  def FluidStackFromIFluidSTack(fluid: IFluidStack): FluidStack = fluid.toMinecraft

  def IItemStorageFromIItemHandler(handler: IItemHandler): IItemStorage = new ItemStorageItemHandler(handler)

  def IItemHandlerModifiableFromIItemStorage(storage: IItemStorage): IItemHandlerModifiable = new WrapperItemHandlerModifiable(storage)

}
