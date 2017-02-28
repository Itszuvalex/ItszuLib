package com.itszuvalex.itszulib.core

import com.itszuvalex.itszulib.TestBase
import com.itszuvalex.itszulib.api.storage.{IItemStorage, ItemStorageArray}
import net.minecraft.util.EnumFacing

/**
  * Created by Christopher Harris (Itszuvalex) on 2/28/17.
  */
class TestSidedStorageConfiguration extends TestBase {

  trait TestConfiguration {
    val invA = new ItemStorageArray(0)
    val invB = new ItemStorageArray(0)
    val invC = new ItemStorageArray(0)
    val keyA = "A"
    val keyB = "B"
    val keyC = "C"
    val map = Map[String, IItemStorage](keyA -> invA, keyB -> invB, keyC -> invC)
    val defaults: (EnumFacing) => String = (f: EnumFacing) => map.keys.toList(f.getIndex % map.size)
    var front = () => EnumFacing.NORTH
    val config = new SidedItemStorageConfiguration(defaults, map, front)
  }

  "TestSidedStorageConfiguration" should {

    "cycleRelativeFacingBackward" should {
      "decrement the key" in new TestConfiguration {
        val facing = EnumFacing.VALUES(1)
        config.getStorageNameForRelativeFacing(facing) shouldBe keyB
        config.cycleRelativeFacingBackward(facing)
        config.getStorageNameForRelativeFacing(facing) shouldBe keyA
      }

      "wrap around 0" in new TestConfiguration {
        val facing = EnumFacing.VALUES(0)
        config.getStorageNameForRelativeFacing(facing) shouldBe keyA
        config.cycleRelativeFacingBackward(facing)
        config.getStorageNameForRelativeFacing(facing) shouldBe keyC
      }
    }

    "cycleRelativeFacingForward" should {
      "increment the key" in new TestConfiguration {
        val facing = EnumFacing.VALUES(1)
        config.getStorageNameForRelativeFacing(facing) shouldBe keyB
        config.cycleRelativeFacingForward(facing)
        config.getStorageNameForRelativeFacing(facing) shouldBe keyC
      }

      "wrap around max" in new TestConfiguration {
        val facing = EnumFacing.VALUES(2)
        config.getStorageNameForRelativeFacing(facing) shouldBe keyC
        config.cycleRelativeFacingForward(facing)
        config.getStorageNameForRelativeFacing(facing) shouldBe keyA
      }
    }

    "getInventoryForGlobalFacing" in new TestConfiguration {

    }


    "getInventoryForRelativeFacing" in new TestConfiguration {

    }

    "getInventoryNameForRelativeFacing" in new TestConfiguration {

    }

    "getInventoryNameForAbsoluteFacing" in new TestConfiguration {

    }

  }
}
