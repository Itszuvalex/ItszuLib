package com.itszuvalex.itszulib.container.sync

import com.itszuvalex.itszulib.api.wrappers.IFluidStack
import com.itszuvalex.itszulib.util.Debug
import net.minecraft.nbt.{NBTBase, NBTTagCompound}
import org.apache.logging.log4j.Level

object SyncIFluidStack {
  def refreshComparison(a: IFluidStack, b: IFluidStack): Boolean = {
    a.isFluidEqual(b) && a.amount == b.amount
  }
}

class SyncIFluidStack
(gui: Int, sync: () => IFluidStack, write: (IFluidStack) => Unit) extends SyncBase[IFluidStack](gui, sync, write, SyncIFluidStack.refreshComparison) {
  cachedValue = IFluidStack.Empty

  override def cache(a: IFluidStack): IFluidStack = a.copy()

  override def writeNBT(): NBTBase = value.serializeNBT()

  override def handleNBT(nbt: NBTBase): Unit = nbt match {
    case comp: NBTTagCompound =>
      value_=(IFluidStack.createFromNBT(comp))
      Debug.log(Level.WARN, "Update fluidstack")
    case _ => value_=(IFluidStack.Empty)
  }
}

