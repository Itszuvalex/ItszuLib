package com.itszuvalex.itszulib.api.storage

import com.itszuvalex.itszulib.api.access.{IFluidAccess, IFluidCollectionAccess}
import net.minecraftforge.fluids.FluidStack

/**
  * Created by Christopher Harris (Itszuvalex) on 3/24/16.
  */
trait IFluidCollectionStorage extends ICollectionStorage[IFluidCollectionStorage, IFluidCollectionAccess, IFluidAccess, FluidStack]

