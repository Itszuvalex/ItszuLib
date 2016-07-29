package com.itszuvalex.itszulib.api.wrappers

import com.itszuvalex.itszulib.TestBase
import com.itszuvalex.itszulib.testing.StubItem
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound

/**
  * Created by Christopher Harris (Itszuvalex) on 7/14/16.
  */
class TestWrapperVanillaItemStack extends TestBase {

  trait DummyItemStack {
    val itemStack = new ItemStack(new StubItem(), 5, 5, new NBTTagCompound)
  }

  "a vanilla wrapper" when {
    "wrapping an itemstack" should {
      "wrap" in new DummyItemStack {
        val wrap = WrapperVanillaItemStack(itemStack)
      }

      "not be empty" in new DummyItemStack {
        val wrap = WrapperVanillaItemStack(itemStack)
        wrap should not be 'Empty
      }

      "return the stack count" in new DummyItemStack {
        val wrap = WrapperVanillaItemStack(itemStack)
        wrap.stackSize shouldEqual itemStack.stackSize
      }

      "be able to set the stack count" in new DummyItemStack {
        val wrap = WrapperVanillaItemStack(itemStack)
        wrap.stackSize shouldEqual itemStack.stackSize
        val prev = wrap.stackSize
        wrap.stackSize = prev + 2
        wrap.stackSize shouldEqual(prev + 2)
        wrap.stackSize shouldEqual itemStack.stackSize
      }

      "return the damage count" in new DummyItemStack {
        val wrap = WrapperVanillaItemStack(itemStack)
        wrap.damage shouldEqual itemStack.getItemDamage
      }

      "be able to set the damage " in new DummyItemStack {
        val wrap = WrapperVanillaItemStack(itemStack)
        wrap.damage shouldEqual itemStack.getItemDamage
        val prev = wrap.damage
        wrap.damage = prev + 2
        wrap.damage shouldEqual(prev + 2)
        wrap.damage shouldEqual itemStack.getItemDamage
      }

      "return the nbt compound" in new DummyItemStack {
        val wrap = WrapperVanillaItemStack(itemStack)
        wrap.nbt should be theSameInstanceAs itemStack.getTagCompound
      }
    }
    "wrapping null" should {
      "wrap" in {
        WrapperVanillaItemStack(null)
      }
      "be empty" in {
        val wrap = WrapperVanillaItemStack(null)
        wrap shouldBe 'Empty
      }
    }
  }
}
