package com.itszuvalex.itszulib.util

import java.util
import java.util.Random

import com.itszuvalex.itszulib.implicits.IDImplicits._
import net.minecraft.entity.item.EntityItem
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.world.World
import net.minecraftforge.oredict.OreDictionary

/**
 * Created by Christopher Harris (Itszuvalex) on 4/6/15.
 */
object InventoryUtils {

  /**
   *
   * This DOES modify slots when attempting to place, regardless of true false.  Thus, passing a copy of the inventory is recommended when testing
   *
   * @param item Item used for matching/stacksize.  Does not modify item
   * @param slots Array of slots to attempt to place item.
   * @param restrictions Array of slot indexs to skip when placing.
   * @return True if slots contains room for item.
   */
  def placeItem(item: ItemStack, slots: Array[ItemStack], restrictions: Array[Int]): Boolean = {
    if (item == null) {
      return true
    }
    var amount = item.stackSize
    if (restrictions != null) {
      util.Arrays.sort(restrictions)
    }
    for (i <- 0 until slots.length) {
      if (restrictions == null || !(util.Arrays.binarySearch(restrictions, i) >= 0)) {
        if (slots(i) != null && compareItem(slots(i), item) == 0) {
          val slot = slots(i)
          val room = slot.getMaxStackSize - slot.stackSize
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
    for (i <- 0 until slots.length) {
      if (restrictions == null || !(util.Arrays.binarySearch(restrictions, i) >= 0)) {
        if (slots(i) == null) {
          slots(i) = item.copy
          slots(i).stackSize = amount
          return true
        }
      }
    }
    false
  }

  /**
   * Drops the item in the world.
   *
   * @param item
   * @param world
   * @param x
   * @param y
   * @param z
   * @param rand
   */
  def dropItem(item: ItemStack, world: World, x: Int, y: Int, z: Int, rand: Random): Unit = {
    if (item == null) return

    val f = rand.nextFloat * 0.8F + 0.1F
    val f1 = rand.nextFloat * 0.8F + 0.1F
    val f2 = rand.nextFloat * 0.8F + 0.1F
    while (item.stackSize > 0) {
      var k1 = rand.nextInt(21) + 10
      if (k1 > item.stackSize) {
        k1 = item.stackSize
      }
      item.stackSize -= k1
      val entityitem = new EntityItem(world,
                                      (x.toFloat + f).toDouble,
                                      (y.toFloat + f1).toDouble,
                                      (z.toFloat + f2).toDouble,
                                      new ItemStack(item.getItem, k1, item.getItemDamage))
      if (item.hasTagCompound) {
        entityitem.getEntityItem.setTagCompound(item.getTagCompound.copy.asInstanceOf[NBTTagCompound])
      }
      val f3 = 0.05F
      entityitem.motionX = (rand.nextGaussian.toFloat * f3).toDouble
      entityitem.motionY = (rand.nextGaussian.toFloat * f3 + 0.2F).toDouble
      entityitem.motionZ = (rand.nextGaussian.toFloat * f3).toDouble
      world.spawnEntityInWorld(entityitem)
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
    var amountLeftToRemove: Int = item.stackSize
    if (amountLeftToRemove <= 0) return true
    if (restrictions != null) {
      util.Arrays.sort(restrictions)
    }
    for (i <- 0 until slots.length) {
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

}
