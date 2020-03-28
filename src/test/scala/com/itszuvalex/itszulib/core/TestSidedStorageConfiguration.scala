package com.itszuvalex.itszulib.core

import com.itszuvalex.itszulib.TestBase
import com.itszuvalex.itszulib.api.storage.{IItemStorage, ItemStorageArray}
import net.minecraft.util.EnumFacing

/**
  * Created by Christopher Harris (Itszuvalex) on 2/28/17.
  */
class TestSidedStorageConfiguration extends TestBase {

  trait TestConfiguration {
    val invA                             = new ItemStorageArray(0)
    val invB                             = new ItemStorageArray(0)
    val invC                             = new ItemStorageArray(0)
    val keyA                             = "A"
    val keyB                             = "B"
    val keyC                             = "C"
    val map                              = Map[String, IItemStorage](keyA -> invA, keyB -> invB, keyC -> invC)
    val defaults: (EnumFacing) => String = (f: EnumFacing) => map.keys.toList(f.getIndex % map.size)
    var front                            = () => EnumFacing.NORTH
    val config                           = new SidedItemStorageConfiguration(defaults, map, front)
  }

  "TestSidedStorageConfiguration" should {

    "cycleRelativeFacingBackward" should {
      "decrement the key" in new TestConfiguration {
        val facing = EnumFacing.VALUES(1)
        config.getStorageNameForRelativeFacing(facing) shouldBe keyB
        config.cycleRelativeFacingStorageBackward(facing)
        config.getStorageNameForRelativeFacing(facing) shouldBe keyA
      }

      "wrap around 0" in new TestConfiguration {
        val facing = EnumFacing.VALUES(0)
        config.getStorageNameForRelativeFacing(facing) shouldBe keyA
        config.cycleRelativeFacingStorageBackward(facing)
        config.getStorageNameForRelativeFacing(facing) shouldBe keyC
      }
    }

    "cycleRelativeFacingForward" should {
      "increment the key" in new TestConfiguration {
        val facing = EnumFacing.VALUES(1)
        config.getStorageNameForRelativeFacing(facing) shouldBe keyB
        config.cycleRelativeFacingStorageForward(facing)
        config.getStorageNameForRelativeFacing(facing) shouldBe keyC
      }

      "wrap around max" in new TestConfiguration {
        val facing = EnumFacing.VALUES(2)
        config.getStorageNameForRelativeFacing(facing) shouldBe keyC
        config.cycleRelativeFacingStorageForward(facing)
        config.getStorageNameForRelativeFacing(facing) shouldBe keyA
      }
    }

    "cycleIOForRelativeFacingForward" should {
      "increment the key" in new TestConfiguration {
        val facing: EnumFacing = EnumFacing.VALUES(1)
        config.getIOForRelativeFacing(facing) shouldBe EnumAutomaticIO.NONE
        config.cycleRelativeFacingIOForward(facing)
        config.getIOForRelativeFacing(facing) shouldBe EnumAutomaticIO.INPUT
      }
      "wrap around" in new TestConfiguration {
        val facing: EnumFacing = EnumFacing.VALUES(1)
        config.getIOForRelativeFacing(facing) shouldBe EnumAutomaticIO.NONE
        config.cycleRelativeFacingIOForward(facing)
        config.getIOForRelativeFacing(facing) shouldBe EnumAutomaticIO.INPUT
        config.cycleRelativeFacingIOForward(facing)
        config.getIOForRelativeFacing(facing) shouldBe EnumAutomaticIO.OUTPUT
        config.cycleRelativeFacingIOForward(facing)
        config.getIOForRelativeFacing(facing) shouldBe EnumAutomaticIO.NONE
      }
    }

    "cycleIOForRelativeFacingBackward" should {
      "wrap around" in new TestConfiguration {
        val facing: EnumFacing = EnumFacing.VALUES(1)
        config.getIOForRelativeFacing(facing) shouldBe EnumAutomaticIO.NONE
        config.cycleRelativeFacingIOBackward(facing)
        config.getIOForRelativeFacing(facing) shouldBe EnumAutomaticIO.OUTPUT
      }
      "decrement the key" in new TestConfiguration {
        val facing: EnumFacing = EnumFacing.VALUES(1)
        config.getIOForRelativeFacing(facing) shouldBe EnumAutomaticIO.NONE
        config.cycleRelativeFacingIOBackward(facing)
        config.getIOForRelativeFacing(facing) shouldBe EnumAutomaticIO.OUTPUT
        config.cycleRelativeFacingIOBackward(facing)
        config.getIOForRelativeFacing(facing) shouldBe EnumAutomaticIO.INPUT
        config.cycleRelativeFacingIOBackward(facing)
        config.getIOForRelativeFacing(facing) shouldBe EnumAutomaticIO.NONE
      }
    }

    "getStorageForGlobalFacing" in new TestConfiguration {
      val facing = EnumFacing.VALUES(1)
      config.getStorageForGlobalFacing(facing) shouldBe invB
    }

    "getStorageForRelativeFacing" in new TestConfiguration {
      val facing = EnumFacing.VALUES(1)
      config.getStorageForRelativeFacing(facing) shouldBe invB
    }

    "getStorageNameForRelativeFacing" in new TestConfiguration {
      val facing = EnumFacing.VALUES(1)
      config.getStorageNameForRelativeFacing(facing) shouldBe keyB
    }

    "getStorageNameForAbsoluteFacing" in new TestConfiguration {
      val facing = EnumFacing.VALUES(1)
      config.getStorageNameForAbsoluteFacing(facing) shouldBe keyB
    }

    "getIOForAbsoluteFacing" in new TestConfiguration {
      val facing = EnumFacing.VALUES(1)
      config.getIOForAbsoluteFacing(facing) shouldBe EnumAutomaticIO.NONE
    }

    "getIOForRelativeFacing" in new TestConfiguration {
      val facing = EnumFacing.VALUES(1)
      config.getIOForRelativeFacing(facing) shouldBe EnumAutomaticIO.NONE
    }

    "SerializeAndDeserializeToNBT" in new TestConfiguration {
      val config2 = new SidedItemStorageConfiguration(defaults, map, front)
      val facing0 = EnumFacing.VALUES(0)
      val facing1 = EnumFacing.VALUES(1)
      val facing2 = EnumFacing.VALUES(2)
      config.getIOForRelativeFacing(facing0) shouldBe EnumAutomaticIO.NONE
      config.getIOForRelativeFacing(facing1) shouldBe EnumAutomaticIO.NONE
      config.getIOForRelativeFacing(facing2) shouldBe EnumAutomaticIO.NONE
      config.getStorageNameForRelativeFacing(facing0) shouldBe keyA
      config.getStorageNameForRelativeFacing(facing1) shouldBe keyB
      config.getStorageNameForRelativeFacing(facing2) shouldBe keyC

      config.cycleRelativeFacingIOForward(facing0)
      config.cycleRelativeFacingIOBackward(facing2)
      config.cycleRelativeFacingStorageForward(facing1)
      config.cycleRelativeFacingStorageBackward(facing2)

      config.getIOForRelativeFacing(facing0) shouldBe EnumAutomaticIO.INPUT
      config.getIOForRelativeFacing(facing1) shouldBe EnumAutomaticIO.NONE
      config.getIOForRelativeFacing(facing2) shouldBe EnumAutomaticIO.OUTPUT

      config.getStorageNameForRelativeFacing(facing1) shouldBe keyC
      config.getStorageNameForRelativeFacing(facing2) shouldBe keyB

      config2.getIOForRelativeFacing(facing0) shouldBe EnumAutomaticIO.NONE
      config2.getIOForRelativeFacing(facing1) shouldBe EnumAutomaticIO.NONE
      config2.getIOForRelativeFacing(facing2) shouldBe EnumAutomaticIO.NONE
      config2.getStorageNameForRelativeFacing(facing0) shouldBe keyA
      config2.getStorageNameForRelativeFacing(facing1) shouldBe keyB
      config2.getStorageNameForRelativeFacing(facing2) shouldBe keyC

      config2.deserializeNBT(config.serializeNBT())

      config2.getIOForRelativeFacing(facing0) shouldBe EnumAutomaticIO.INPUT
      config2.getIOForRelativeFacing(facing1) shouldBe EnumAutomaticIO.NONE
      config2.getIOForRelativeFacing(facing2) shouldBe EnumAutomaticIO.OUTPUT

      config2.getStorageNameForRelativeFacing(facing1) shouldBe keyC
      config2.getStorageNameForRelativeFacing(facing2) shouldBe keyB
    }
  }
}
