package com.itszuvalex.itszulib.core

import com.itszuvalex.itszulib.TestBase
import com.itszuvalex.itszulib.api.core.{IModule, Module}
import net.minecraft.util.EnumFacing

class TestModuleCapabilityMap extends TestBase {

  trait TestMap {
    Module.clear()
    val capmap                    = new ModuleCapabilityMap
    val emptyModule: IModule[Int] = Module.registerModule("TestModule", null)
  }

  "TestModuleCapabilityMap" should {
    "register and have module" in new TestMap {
      capmap.addModule(emptyModule, _ => Some(0))
      capmap.hasModule(emptyModule, null) shouldBe true
      EnumFacing.VALUES.foreach(facing =>
                                  capmap.hasModule(emptyModule, facing) shouldBe true)
    }

    "return the correct module value when asked" in new TestMap {
      capmap.addModule(emptyModule, facing => if (facing != null) Some(facing.getIndex) else None)
      capmap.hasModule(emptyModule, null) shouldBe false
      EnumFacing.VALUES.foreach { facing =>
        capmap.hasModule(emptyModule, facing) shouldBe true
        capmap.getModule(emptyModule, facing) shouldBe facing.getIndex
      }
    }
  }

}
