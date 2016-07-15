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



}
