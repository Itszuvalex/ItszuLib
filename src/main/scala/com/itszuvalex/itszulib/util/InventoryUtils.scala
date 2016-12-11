package com.itszuvalex.itszulib.util

import java.util
import java.util.Random

import com.itszuvalex.itszulib.api.core.Loc4
import com.itszuvalex.itszulib.implicits.IDImplicits._
import net.minecraft.entity.item.EntityItem
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraftforge.oredict.OreDictionary

/**
  * Created by Christopher Harris (Itszuvalex) on 4/6/15.
  */
object InventoryUtils {

  /**
    *
    * This DOES modify slots when attempting to place, regardless of true false.  Thus, passing a copy of the inventory is recommended when testing
    *
    * @param item         Item used for matching/stacksize.  Does not modify item
    * @param slots        Array of slots to attempt to place item.
    * @param restrictions Array of slot indexs to skip when placing.
    * @return True if slots contains room for item.
    */
  def placeItem(item: ItemStack, slots: Array[ItemStack], restrictions: Array[Int]): Boolean = {
    if (item == null) {
      return true
    }
    var amount = item.func_190916_E
    if (restrictions != null) {
      util.Arrays.sort(restrictions)
    }
    slots.indices.foreach { i =>
      if (restrictions == null || !(util.Arrays.binarySearch(restrictions, i) >= 0)) {
        if (slots(i) != null && compareItem(slots(i), item) == 0) {
          val slot = slots(i)
          val room = slot.getMaxStackSize - slot.func_190916_E
          if (room < amount) {
            slot.func_190920_e(slot.func_190916_E() + room)
            amount -= room
          } else {
            slot.func_190920_e(slot.func_190916_E() + amount)
            return true
          }
        }
      }
                          }
    slots.indices.foreach { i =>
      if (restrictions == null || !(util.Arrays.binarySearch(restrictions, i) >= 0)) {
        if (slots(i) == null) {
          slots(i) = item.copy
          slots(i).func_190920_e(amount)
          return true
        }
      }
                          }
    false
  }

  def compareItem(cur: ItemStack, in: ItemStack): Int = {
    if (cur == null && in != null) {
      return -1
    }
    if (cur != null && in == null) {
      return 1
    }
    if (cur == null && in == null) {
      return 0
    }
    if (cur.getItem.itemID < in.getItem.itemID) {
      return -1
    }
    if (cur.getItem.itemID > in.getItem.itemID) {
      return 1
    }
    val damage = cur.getItemDamage
    val indamage = in.getItemDamage
    if (damage == OreDictionary.WILDCARD_VALUE) return 0
    if (indamage == OreDictionary.WILDCARD_VALUE) return 0
    if (damage < indamage) {
      return -1
    }
    if (damage > indamage) {
      return 1
    }
    0
  }

  /**
    * Drops the item in the world.
    *
    * @param item
    * @param loc
    * @param rand
    */
  def dropItem(item: ItemStack, loc: Loc4, rand: Random): Unit = {
    if (item == null) return

    val f = rand.nextFloat * 0.8F + 0.1F
    val f1 = rand.nextFloat * 0.8F + 0.1F
    val f2 = rand.nextFloat * 0.8F + 0.1F

    while (item.func_190916_E > 0) {
      var k1 = rand.nextInt(21) + 10
      if (k1 > item.func_190916_E) {
        k1 = item.func_190916_E
      }
      val dstack = new ItemStack(item.serializeNBT())
      dstack.func_190920_e(k1)
      val entityItem = new EntityItem(loc.getWorld.get,
        (loc.getPos.getX.toFloat + f).toDouble,
        (loc.getPos.getY.toFloat + f1).toDouble,
        (loc.getPos.getZ.toFloat + f2).toDouble,
        dstack)
      item.func_190920_e(item.func_190916_E() - k1)
      if (item.hasTagCompound) {
        entityItem.getEntityItem.setTagCompound(item.getTagCompound.copy)
      }
      val f3 = 0.05F
      entityItem.motionX = (rand.nextGaussian.toFloat * f3).toDouble
      entityItem.motionY = (rand.nextGaussian.toFloat * f3 + 0.2F).toDouble
      entityItem.motionZ = (rand.nextGaussian.toFloat * f3).toDouble
      loc.getWorld.get.spawnEntityInWorld(entityItem)
    }
  }

  /**
    *
    * This DOES modify slots when attempting to place, regardless of output.  Thus, it is recommended to pass a copy when testing.
    *
    * @param item Item used to attempt to place.  This is NOT modified.
    * @param slots
    * @param restrictions
    * @return
    */
  def removeItem(item: ItemStack, slots: Array[ItemStack], restrictions: Array[Int]): Boolean = {
    if (item == null) {
      return true
    }
    var amountLeftToRemove: Int = item.func_190916_E
    if (amountLeftToRemove <= 0) return true
    if (restrictions != null) {
      util.Arrays.sort(restrictions)
    }
    slots.indices.foreach { i =>
      if (restrictions == null || !(util.Arrays.binarySearch(restrictions, i) >= 0)) {
        if (slots(i) != null && compareItem(slots(i), item) == 0) {
          val slot = slots(i)
          val amount = slot.func_190916_E
          if (amount <= amountLeftToRemove) {
            slots(i) = null
            amountLeftToRemove -= amount
            if (amountLeftToRemove <= 0) {
              return true
            }
          } else {
              slot.func_190920_e(slot.func_190916_E() - amountLeftToRemove)
            return true
          }
        }
      }
    }
    false
  }

}
