package com.itszuvalex.itszulib.container.sync

import com.itszuvalex.itszulib.api.wrappers.IItemStack
import net.minecraft.nbt.{NBTBase, NBTTagCompound}

/**
  * Created by Chris on 12/13/2016.
  */
class SyncIItemStack(sync: () => IItemStack, write: (IItemStack) => Unit) extends SyncBase[IItemStack](sync, write, IItemStack.itemStackEquality.apply) {
  cachedValue = IItemStack.Empty

  override def writeNBT(): NBTBase = value.serializeNBT()

  override def handleNBT(nbt: NBTBase): Unit = nbt match {
    case comp: NBTTagCompound =>
      value = IItemStack.createFromNBT(comp)
    case _ => value = IItemStack.Empty
  }
}
