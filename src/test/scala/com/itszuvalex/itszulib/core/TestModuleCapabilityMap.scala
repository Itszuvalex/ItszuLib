package com.itszuvalex.itszulib.core

import com.itszuvalex.itszulib.TestBase
import com.itszuvalex.itszulib.api.core.Module
import net.minecraft.nbt.{NBTBase, NBTTagCompound}
import net.minecraft.util.EnumFacing
import net.minecraftforge.common.capabilities.Capability

class TestModuleCapabilityMap extends TestBase {

  trait TestMap {
    val capmap      = new ModuleCapabilityMap
    val emptyModule = new Module[Int]()
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
