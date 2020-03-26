package com.itszuvalex.itszulib.api.multiblock

import com.itszuvalex.itszulib.TestBase
import com.itszuvalex.itszulib.api.wrappers.IWorld
import net.minecraft.util.Rotation
import net.minecraft.util.math.BlockPos

class TestIBlockPattern extends TestBase {

  trait withTestPattern {
    val testMap                          = Array(new BlockPos(0, 0, 0), new BlockPos(0, 0, 1), new BlockPos(1, 0, 0), new BlockPos(1, 0, 1))
    var allowedFunc: BlockPos => Boolean = (_ => true)
    val testPattern                      = new TestableBlockPattern(testMap, x => allowedFunc(x))
  }

  "A IBlockPattern" should {
    "overChunkBoundaries" should {
      "return false if pattern over chunk boundaries" in new withTestPattern {
        testPattern.overChunkBoundaries(null, new BlockPos(14, 0, 14)) shouldBe false
        testPattern.overChunkBoundaries(null, new BlockPos(16, 0, 16)) shouldBe false
      }
      "return true if pattern over chunk boundaries" in new withTestPattern {
        testPattern.overChunkBoundaries(null, new BlockPos(15, 0, 15)) shouldBe true
      }
    }
  }


  class TestableBlockPattern(var locs: Iterable[BlockPos], val allowed: BlockPos => Boolean, rot: Rotation = Rotation.NONE) extends IBlockPattern {
    override def rotation: Rotation = rot

    override def canRotate: Boolean = false

    override def rotate(rot: Rotation): Unit = {}

    override def matches(world: IWorld, pos: BlockPos): Boolean = locs.forall(x => allowed(pos.add(x)))

    /**
      *
      * @param world
      * @param pos
      * @return Iterable of actual world BlockPos of all blocks that would be used to make a multiblock with controller at pos
      */
    override def blocksInMatch(world: IWorld, pos: BlockPos): Iterable[BlockPos] = locs.map(x => pos.add(x))

    override def copy(): IBlockPattern = new TestableBlockPattern(locs, allowed, rot)
  }

}
