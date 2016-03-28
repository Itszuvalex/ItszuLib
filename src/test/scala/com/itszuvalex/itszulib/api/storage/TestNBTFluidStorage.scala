package com.itszuvalex.itszulib.api.storage

import com.itszuvalex.itszulib.TestBase
import com.itszuvalex.itszulib.api.access.AccessHelpers
import com.itszuvalex.itszulib.util.Comparators._
import net.minecraft.nbt.NBTTagCompound
import org.scalatest.BeforeAndAfterAll

/**
  * Created by Christopher Harris (Itszuvalex) on 3/13/2016.
  */
/*
class TestNBTFluidStorage extends TestBase with BeforeAndAfterAll {

  override protected def beforeAll(): Unit = {
    NBTFluidStorage.setNBTFluidDeserializer(AccessHelpers.testNBTFluidDeserializer)
    NBTFluidStorage.setNBTFluidSerializer(AccessHelpers.testNBTFluidSerializer)
    super.beforeAll()
  }

  override protected def afterAll(): Unit = {
    NBTFluidStorage.restoreDefaultNBTFluidDeserializer()
    NBTFluidStorage.restoreDefaultNBTFluidSerializer()
    super.afterAll()
  }

  trait EmptyNBT extends AccessHelpers.EmptyFluidArray {
    val compound = new NBTTagCompound
    compound.setInteger(NBTFluidStorage.SIZE_KEY, array.length)
  }

  trait PartialNBT extends AccessHelpers.PartialFluidArray {
    val compound = new NBTTagCompound
    compound.setInteger(NBTFluidStorage.SIZE_KEY, array.length)
    array.zipWithIndex.foreach { case (item, index) =>
      if (item != null) {
        val c = new NBTTagCompound
        NBTFluidStorage.serialize(item, c)
        compound.setTag(index.toString, c)
      }
                               }
  }

  trait EmptyStorage extends EmptyNBT {
    val storage = new NBTFluidStorage(compound, true)
  }

  trait PartialStorage extends PartialNBT {
    val storage = new NBTFluidStorage(compound)
  }

  "An array fluid storage" should {
    "have access" in new EmptyStorage {
      storage.getFullAccess should not be null
    }
    "correctly serialize and deserialize to NBT" in new PartialStorage {
      val comp = new NBTTagCompound
      storage.saveToNBT(comp)
      val other = new ArrayFluidStorage(0)
      other.loadFromNBT(comp)

      storage.getFullAccess.length shouldEqual other.getFullAccess.length

      storage.getFullAccess(0) should not be 'Empty
      other.getFullAccess(0) should not be 'Empty

      storage.getFullAccess.indices.forall { i =>
        FluidStack.IDNBTComparator.compare(storage.getFullAccess(i).get.orNull,
                                           other.getFullAccess(i).get.orNull) == 0
                                           }
    }
  }
}
*/
