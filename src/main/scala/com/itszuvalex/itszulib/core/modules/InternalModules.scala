package com.itszuvalex.itszulib.core.modules

import com.itszuvalex.itszulib.api.core.{IModule, Module}

object InternalModules {
  val MODULE_DROP_INVENTORY: IModule[ModuleDropInventory] = Module.registerModule[ModuleDropInventory]("ModuleDropInventory", null)
  val MODULE_ITEM_AUTO_IO  : IModule[ModuleIItemAutoIO]   = Module.registerModule[ModuleIItemAutoIO]("ModuleItemAutoIO", null)
  val MODULE_FLUID_AUTO_IO : IModule[ModuleIFluidAutoIO]  = Module.registerModule[ModuleIFluidAutoIO]("ModuleFluidAutoIO", null)
  val MODULE_GUI           : IModule[ModuleGui]           = Module.registerModule[ModuleGui]("ModuleGui", null)
}
