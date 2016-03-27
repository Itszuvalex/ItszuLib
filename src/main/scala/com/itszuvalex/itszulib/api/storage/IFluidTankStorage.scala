package com.itszuvalex.itszulib.api.storage

import com.itszuvalex.itszulib.api.access.{IFluidTankAccess, IFluidTankCollectionAccess}
import net.minecraftforge.fluids.FluidTank

/**
  * Created by Christopher Harris (Itszuvalex) on 3/27/2016.
  */
trait IFluidTankStorage extends IStorage[IFluidTankStorage, IFluidTankCollectionAccess, IFluidTankAccess, FluidTank]


