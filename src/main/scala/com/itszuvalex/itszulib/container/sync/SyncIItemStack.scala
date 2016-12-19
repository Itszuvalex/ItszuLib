package com.itszuvalex.itszulib.container.sync

import com.itszuvalex.itszulib.api.wrappers.IItemStack
import com.itszuvalex.itszulib.util.Debug
import net.minecraft.nbt.{NBTBase, NBTTagCompound}
import org.apache.logging.log4j.Level

/**
  * Created by Chris on 12/13/2016.
  */
object SyncIItemStack {
  def refreshComparison(a: IItemStack, b: IItemStack): Boolean = {
    IItemStack.itemStackEquality.apply(a, b) match {
      case true if a.isEmpty || b.isEmpty => true
      case true => a.stackSize == b.stackSize
      case false => false
    }
  }
}

class SyncIItemStack(sync: () => IItemStack, write: (IItemStack) => Unit) extends SyncBase[IItemStack](sync, write, SyncIItemStack.refreshComparison) {
  cachedValue = IItemStack.Empty

  override def cache(a: IItemStack): IItemStack = a.copy()

  override def writeNBT(): NBTBase = value.serializeNBT()

  override def handleNBT(nbt: NBTBase): Unit = nbt match {
    case comp: NBTTagCompound =>
      value_=(IItemStack.createFromNBT(comp))
      Debug.log(Level.WARN, "Update itemstack")
    case _ => value_=(IItemStack.Empty)
  }
}
