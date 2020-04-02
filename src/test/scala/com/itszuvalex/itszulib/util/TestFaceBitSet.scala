package com.itszuvalex.itszulib.util

import com.itszuvalex.itszulib.TestBase
import net.minecraft.util.EnumFacing

class TestFaceBitSet extends TestBase {

  trait test {
    val bitset                         = new FaceBitSet
    val facetestset: Array[EnumFacing] = Array(null) ++ EnumFacing.VALUES
  }

  "A FaceBitSet" should {
    "construct with false values and handle a null EnumFacing" in new test {
      facetestset.map(bitset.get).foreach(_ shouldBe false)
    }

    "set individual bits" in new test {
      facetestset.foreach { f =>
        val bitset = new FaceBitSet
        bitset.set(f)

        facetestset.foreach(t => bitset.get(t) shouldBe (t == f))
      }
    }

    "clear individual bits" in new test {
      facetestset.foreach(bitset.set)
      facetestset.forall(bitset.get) shouldBe true
      facetestset.foreach { f =>
        bitset.clear(f)
        facetestset.foreach(t => bitset.get(t) shouldBe (t != f))
        bitset.set(f)
        bitset.get(f) shouldBe true
      }
    }

    "toggle individual bits on" in new test {
      facetestset.foreach { f =>
        val bitset = new FaceBitSet
        bitset.toggle(f)
        facetestset.foreach(t => bitset.get(t) shouldBe (t == f))
      }
    }

    "toggle individual bits off" in new test {
      facetestset.foreach(bitset.set)
      facetestset.forall(bitset.get) shouldBe true
      facetestset.foreach { f =>
        bitset.toggle(f)
        facetestset.foreach(t => bitset.get(t) shouldBe (t != f))
        bitset.set(f)
        bitset.get(f) shouldBe true
      }
    }

    "setTo should select the right function for true" in new test {
      facetestset.foreach { f =>
        val bitset = new FaceBitSet
        bitset.setTo(f, true)

        facetestset.foreach(t => bitset.get(t) shouldBe (t == f))
      }
    }

    "setTo should select the right function for false" in new test {
      facetestset.foreach(bitset.set)
      facetestset.forall(bitset.get) shouldBe true
      facetestset.foreach { f =>
        bitset.setTo(f, false)
        facetestset.foreach(t => bitset.get(t) shouldBe (t != f))
        bitset.set(f)
        bitset.get(f) shouldBe true
      }
    }
  }
}
