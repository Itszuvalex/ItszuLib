package com.itszuvalex.itszulib.api.storage

import com.itszuvalex.itszulib.api.wrappers.IFluidStack

class DynamicIFluidStorageModifiable(val gettermod: () => IFluidStorageModifiable) extends DynamicIFluidStorage(gettermod) with IFluidStorageModifiable {
  override def update(i: Int, s: IFluidStack): Unit = gettermod().update(i, s)
}
