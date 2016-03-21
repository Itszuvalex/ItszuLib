package com.itszuvalex.itszulib.implicits

import com.itszuvalex.itszulib.util.StringUtils
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraftforge.oredict.OreDictionary

import scala.collection.JavaConversions._

/**
  * Created by Christopher Harris (Itszuvalex) on 10/11/14.
  */
object ItemStackImplicits {

  implicit class ItemStackImplicits(i: ItemStack) {
    def toModQualifiedString: String = StringUtils.itemStackToString(i)
  }

  implicit class StringItemStackImplicits(i: String) {
    def toItemStack: ItemStack = StringUtils.itemStackFromString(i)
  }


  implicit class ItemStackArrayImplicits(i: Array[ItemStack]) {
    def deepCopy: Array[ItemStack] = i.map(f => if (f == null) null else f.copy)
  }

  implicit class ItemStackOreDictionaryComparison(item: ItemStack) {
    def ==(oreDictionary: String) = isOre(oreDictionary)

    def isOre(oreDictionary: String) = OreDictionary.getOres(oreDictionary)
                                       .exists(ItemStack.areItemStacksEqual(_, item))
  }

  implicit class ForcedNBT(i: ItemStack) {
    def forceTag: NBTTagCompound = {
      if (!i.hasTagCompound)
        i.stackTagCompound = new NBTTagCompound
      i.getTagCompound
    }
  }

}
