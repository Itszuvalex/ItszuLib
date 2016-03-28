package com.itszuvalex.itszulib.api.access

import com.itszuvalex.itszulib.api.storage.IFluidTankCollectionStorage
import net.minecraftforge.fluids.FluidTank

/**
  * Created by Christopher Harris (Itszuvalex) on 3/24/16.
  */
class StorageFluidTankAccess(private val storage: IFluidTankCollectionStorage, private val index: Int) extends IFluidTankAccess {
  private val revision = storage.getRevision

  /**
    * Don't use unless absolutely necessary
    *
    * @return Backing FluidStack
    */
  override def get: Option[FluidTank] = if (isValid) Option(storage(index)) else None

  /**
    *
    * @return True if this access is still valid.  False if underlying storage is no longer correct.
    */
  override def isValid: Boolean = revision == storage.getRevision

  /**
    * Sets this item access's storage to the FluidStack.
    *
    * @param tank FluidStack to set this to.
    */
  override def set(tank: FluidTank): Unit = {
    storage(index) = tank
    onChanged()
  }
}
