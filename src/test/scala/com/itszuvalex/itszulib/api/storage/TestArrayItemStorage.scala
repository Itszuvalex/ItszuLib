package com.itszuvalex.itszulib.api.storage

import com.itszuvalex.itszulib.TestBase
import com.itszuvalex.itszulib.api.access.{AccessHelpers, NBTItemAccess}
import com.itszuvalex.itszulib.testing.StubItem
import com.itszuvalex.itszulib.util.Comparators.ItemStack.IDDamageWildCardNBTComparator
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import org.scalatest.BeforeAndAfterEach

/**
  * Created by Christopher Harris (Itszuvalex) on 3/13/2016.
  */
class TestArrayItemStorage extends TestBase with BeforeAndAfterEach {

  override protected def beforeEach(): Unit = {
    NBTItemAccess.setNBTItemDeserializer({ nbt =>
      if (nbt.hasNoTags)
        null
      else {
        val item = new ItemStack(new StubItem())
        item.stackSize = nbt.getByte("Count")
        item.setItemDamage(nbt.getShort("Damage"))

        if (item.getItemDamage < 0) {
          item.setItemDamage(0)
        }
        item
      }
                                         })
  }

  override protected def afterEach(): Unit = {
    NBTItemAccess.restoreDefaultNBTItemDeserializer()
  }

  trait EmptyStorage extends AccessHelpers.EmptyArray {
    val storage = new ArrayItemStorage(array)
  }

  trait PartialStorage extends AccessHelpers.PartialArray {
    val storage = new ArrayItemStorage(array)
  }

  "An array item storage" should {
    "have access and inventory " in new EmptyStorage {
      storage.getAccess should not be null
      storage.getInventory should not be null
    }
    "have the same size" in new EmptyStorage {
      storage.getAccess.length shouldEqual storage.getInventory.getSizeInventory
    }
    "have matching items" in new PartialStorage {
      storage.getAccess(0) should not be 'Empty

      storage.getAccess.indices.forall { i =>
        IDDamageWildCardNBTComparator.compare(storage.getAccess(i).getItemStack.orNull,
                                              storage.getInventory.getStackInSlot(i)) == 0
                                       }
    }
    "correctly serialize and deserialize to NBT" in new PartialStorage {
      val comp = new NBTTagCompound
      storage.saveToNBT(comp)
      val other = new ArrayItemStorage(0)
      other.loadFromNBT(comp)

      storage.getAccess.length shouldEqual other.getAccess.length

      storage.getAccess(0) should not be 'Empty
      other.getAccess(0) should not be 'Empty

      storage.getAccess.indices.forall { i =>
        IDDamageWildCardNBTComparator.compare(storage.getAccess(i).getItemStack.orNull,
                                              other.getAccess(i).getItemStack.orNull) == 0
                                       }
    }
  }
}
