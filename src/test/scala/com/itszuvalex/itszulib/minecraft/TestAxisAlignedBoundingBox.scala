package com.itszuvalex.itszulib.minecraft

import com.itszuvalex.itszulib.TestBase
import net.minecraft.util.math.{AxisAlignedBB, Vec3d}

class TestAxisAlignedBoundingBox extends TestBase {

  "An AxisAlignedBB" should {
    "not contain a vector on its edge" in {
      val bb = new AxisAlignedBB(0, 0, 0, 1, 1, 1)
      val hit = bb.calculateIntercept(new Vec3d(.5, .5, .5), new Vec3d(1.5, .5, .5))
      (hit == null) shouldBe false
      bb.contains(hit.hitVec) shouldBe false
      bb.grow(1.01).contains(hit.hitVec) shouldBe true
    }
  }
}
