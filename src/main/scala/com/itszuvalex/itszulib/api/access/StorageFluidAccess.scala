package com.itszuvalex.itszulib.api.access

import com.itszuvalex.itszulib.api.storage.IFluidCollectionStorage
import net.minecraftforge.fluids.FluidStack

/**
  * Created by Christopher Harris (Itszuvalex) on 3/24/16.
  */
class StorageFluidAccess(private val storage: IFluidCollectionStorage, private val index: Int) extends IFluidAccess {
  private val revision = storage.getRevision

  /**
    * Don't use unless absolutely necessary
    *
    * @return Backing FluidStack
    */
  override def get: Option[FluidStack] = if (isValid) Option(storage(index)) else None

  /**
    *
    * @return True if this access is still valid.  False if underlying storage is no longer correct.
    */
  override def isValid: Boolean = revision == storage.getRevision

  /**
    * Sets this item access's storage to the FluidStack.
    *
    * @param stack FluidStack to set this to.
    */
  override def set(stack: FluidStack): Unit = {
    storage(index) = stack
    onChanged()
  }
}
