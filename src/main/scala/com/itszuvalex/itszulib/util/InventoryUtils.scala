package com.itszuvalex.itszulib.util

import java.util
import java.util.Random

import com.itszuvalex.itszulib.api.core.Loc4
import com.itszuvalex.itszulib.api.wrappers.IItemStack
import net.minecraft.entity.item.EntityItem
import net.minecraft.item.ItemStack
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
    *
    * @return True if slots contains room for item.
    */
  def placeItem(item: IItemStack, slots: Array[IItemStack], restrictions: Array[Int]): Boolean = {
    if (item == null || item.isEmpty) {
      return true
    }
    var amount = item.stackSize
    if (restrictions != null) {
      util.Arrays.sort(restrictions)
    }
    slots.indices.foreach { i =>
      if (restrictions == null || !(util.Arrays.binarySearch(restrictions, i) >= 0)) {
        if (slots(i) != null && compareItem(slots(i), item) == 0) {
          val slot = slots(i)
          val room = slot.stackSizeMax - slot.stackSize
          if (room < amount) {
            slot.stackSize += room
            amount -= room
          } else {
            slot.stackSize += amount
            return true
          }
        }
      }
    }
    slots.indices.foreach { i =>
      if (restrictions == null || !(util.Arrays.binarySearch(restrictions, i) >= 0)) {
        if (slots(i) == null) {
          slots(i) = item.copy()
          slots(i).stackSize = amount
          return true
        }
      }
    }
    false
  }

  def compareItem(cur: IItemStack, in: IItemStack): Int = {
    if (cur == null && in != null) {
      return -1
    }
    if (cur != null && in == null) {
      return 1
    }
    if (cur == null && in == null) {
      return 0
    }
    if (cur.itemID < in.itemID) {
      return -1
    }
    if (cur.itemID > in.itemID) {
      return 1
    }
    val damage = cur.damage
    val indamage = in.damage
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
  def dropItem(item: IItemStack, loc: Loc4, rand: Random): Unit = {
    if (item == null) return
    if (item.isEmpty) return

    val f = rand.nextFloat * 0.8F + 0.1F
    val f1 = rand.nextFloat * 0.8F + 0.1F
    val f2 = rand.nextFloat * 0.8F + 0.1F

    while (item.stackSize > 0) {
      var k1 = rand.nextInt(21) + 10
      if (k1 > item.stackSize) {
        k1 = item.stackSize
      }
      val dstack = new ItemStack(item.serializeNBT())
      dstack.setCount(k1)
      val entityItem = new EntityItem(loc.getWorld.get,
        (loc.getPos.getX.toFloat + f).toDouble,
        (loc.getPos.getY.toFloat + f1).toDouble,
        (loc.getPos.getZ.toFloat + f2).toDouble,
        dstack)
      item.stackSize -= k1
      if (item.hasNbt) {
        entityItem.getEntityItem.setTagCompound(item.nbt.copy)
      }
      val f3 = 0.05F
      entityItem.motionX = (rand.nextGaussian.toFloat * f3).toDouble
      entityItem.motionY = (rand.nextGaussian.toFloat * f3 + 0.2F).toDouble
      entityItem.motionZ = (rand.nextGaussian.toFloat * f3).toDouble
      loc.getWorld.get.spawnEntity(entityItem)
    }
  }

  /**
    *
    * This DOES modify slots when attempting to place, regardless of output.  Thus, it is recommended to pass a copy when testing.
    *
    * @param item Item used to attempt to place.  This is NOT modified.
    * @param slots
    * @param restrictions
    *
    * @return
    */
  def removeItem(item: IItemStack, slots: Array[IItemStack], restrictions: Array[Int]): Boolean = {
    if (item == null || item.isEmpty) {
      return true
    }
    var amountLeftToRemove: Int = item.stackSize
    if (amountLeftToRemove <= 0) return true
    if (restrictions != null) {
      util.Arrays.sort(restrictions)
    }
    slots.indices.foreach { i =>
      if (restrictions == null || !(util.Arrays.binarySearch(restrictions, i) >= 0)) {
        if (slots(i) != null && compareItem(slots(i), item) == 0) {
          val slot = slots(i)
          val amount = slot.stackSize
          if (amount <= amountLeftToRemove) {
            slots(i) = null
            amountLeftToRemove -= amount
            if (amountLeftToRemove <= 0) {
              return true
            }
          } else {
            slot.stackSize -= amountLeftToRemove
            return true
          }
        }
      }
    }
    false
  }

}
