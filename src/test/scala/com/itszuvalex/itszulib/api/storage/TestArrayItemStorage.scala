package com.itszuvalex.itszulib.api.storage

import com.itszuvalex.itszulib.TestBase
import com.itszuvalex.itszulib.api.access.AccessHelpers
import com.itszuvalex.itszulib.util.Comparators.ItemStack.IDDamageWildCardNBTComparator
import net.minecraft.nbt.NBTTagCompound
import org.scalatest.BeforeAndAfterAll

/**
  * Created by Christopher Harris (Itszuvalex) on 3/13/2016.
  */
class TestArrayItemStorage extends TestBase with BeforeAndAfterAll {

  override protected def beforeAll(): Unit = {
    NBTItemStorage.setNBTItemDeserializer(AccessHelpers.testNBTItemDeserializer)
    super.beforeAll()
  }


  override protected def afterAll(): Unit = {
    NBTItemStorage.restoreDefaultNBTItemDeserializer()
    super.afterAll()
  }

  trait EmptyStorage extends AccessHelpers.EmptyItemArray {
    val storage = new ArrayItemStorage(array)
  }

  trait PartialStorage extends AccessHelpers.PartialItemArray {
    val storage = new ArrayItemStorage(array)
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
        IDDamageWildCardNBTComparator.compare(storage.getFullAccess(i).get.orNull,
                                              storage.getInventory.getStackInSlot(i)) == 0
                                       }
    }
    "correctly serialize and deserialize to NBT" in new PartialStorage {
      val comp = new NBTTagCompound
      storage.saveToNBT(comp)
      val other = new ArrayItemStorage(0)
      other.loadFromNBT(comp)

      storage.getFullAccess.length shouldEqual other.getFullAccess.length

      storage.getFullAccess(0) should not be 'Empty
      other.getFullAccess(0) should not be 'Empty

      storage.getFullAccess.indices.forall { i =>
        IDDamageWildCardNBTComparator.compare(storage.getFullAccess(i).get.orNull,
                                              other.getFullAccess(i).get.orNull) == 0
                                       }
    }
  }
}
