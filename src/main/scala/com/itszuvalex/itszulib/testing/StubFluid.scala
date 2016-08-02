package com.itszuvalex.itszulib.testing

import net.minecraftforge.fluids.{Fluid, FluidRegistry}

/**
  * Created by Christopher Harris (Itszuvalex) on 3/27/2016.
  */
object StubFluid {
  def make(name: String = "Test") = {
    val fluid = new StubFluid(name)
    FluidRegistry.registerFluid(fluid)
    fluid
  }
}

class StubFluid(val name: String = "Test") extends Fluid(name, null, null)
