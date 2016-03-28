package com.itszuvalex.itszulib.api.storage

import com.itszuvalex.itszulib.api.access.{IFluidTankAccess, IFluidTankCollectionAccess}
import net.minecraftforge.fluids.FluidTank

/**
  * Created by Christopher Harris (Itszuvalex) on 3/27/2016.
  */
trait IFluidTankCollectionStorage extends ICollectionStorage[IFluidTankCollectionStorage, IFluidTankCollectionAccess, IFluidTankAccess, FluidTank]


