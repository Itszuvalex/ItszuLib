package com.itszuvalex.itszulib.api.storage

import com.itszuvalex.itszulib.TestBase
import com.itszuvalex.itszulib.api.access.AccessHelpers
import com.itszuvalex.itszulib.util.Comparators.ItemStack.IDDamageWildCardNBTComparator
import net.minecraft.nbt.NBTTagCompound
import org.scalatest.BeforeAndAfterAll

/**
  * Created by Christopher Harris (Itszuvalex) on 3/13/2016.
  */
class TestNBTItemStorage extends TestBase with BeforeAndAfterAll {

  override protected def beforeAll(): Unit = {
    NBTItemCollectionStorage.setNBTItemDeserializer(AccessHelpers.testNBTItemDeserializer)
    super.beforeAll()
  }

  trait EmptyNBT extends AccessHelpers.EmptyItemArray {
    val compound = new NBTTagCompound
    compound.setInteger(NBTItemCollectionStorage.SIZE_KEY, array.length)
  }

  trait PartialNBT extends AccessHelpers.PartialItemArray {
    val compound = new NBTTagCompound
    compound.setInteger(NBTItemCollectionStorage.SIZE_KEY, array.length)
    array.zipWithIndex.foreach { case (item, index) =>
      if (item != null) {
        val c = new NBTTagCompound
        item.writeToNBT(c)
        compound.setTag(index.toString, c)
      }
                               }
  }

  override protected def afterAll(): Unit = {
    NBTItemCollectionStorage.restoreDefaultNBTItemDeserializer()
    super.afterAll()
  }

  trait EmptyStorage extends EmptyNBT {
    val storage = new NBTItemCollectionStorage(compound, true)
  }

  trait PartialStorage extends PartialNBT {
    val storage = new NBTItemCollectionStorage(compound)
  }

  "An array item storage" should {
    "have access and inventory " in new EmptyStorage {
      storage.getFullAccess should not be null
      storage.getInventory should not be null
    }
    "have the same size" in new EmptyStorage {
      storage.getFullAccess.length shouldEqual storage.getInventory.getSizeInventory
    }
    "have matching items" in new PartialStorage {
      storage.getFullAccess(0) should not be 'Empty

      storage.getFullAccess.indices.forall { i =>
        IDDamageWildCardNBTComparator.compare(storage.getFullAccess(i).get.flatMap(_.toMinecraft).orNull,
                                              storage.getInventory.getStackInSlot(i)) == 0
                                           }
    }
    "correctly serialize and deserialize to NBT" in new PartialStorage {
      val comp  = storage.serializeNBT()
      val other = new ArrayItemCollectionStorage(0)
      other.deserializeNBT(comp)

      storage.getFullAccess.length shouldEqual other.getFullAccess.length

      storage.getFullAccess(0) should not be 'Empty
      other.getFullAccess(0) should not be 'Empty

      storage.getFullAccess.indices.forall { i =>
        IDDamageWildCardNBTComparator.compare(storage.getFullAccess(i).get.flatMap(_.toMinecraft).orNull,
                                              other.getFullAccess(i).get.flatMap(_.toMinecraft).orNull) == 0
                                           }
    }
  }
}
