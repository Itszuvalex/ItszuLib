package com.itszuvalex.itszulib.implicits

import com.itszuvalex.itszulib.api.wrappers.{Converter, IItemStack}
import com.itszuvalex.itszulib.util.StringUtils
import net.minecraft.block.Block
import net.minecraft.item.{Item, ItemStack}
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
    def ==(oreDictionary: String): Boolean = isOre(oreDictionary)

    def isOre(oreDictionary: String): Boolean = OreDictionary.getOres(oreDictionary)
                                                             .exists(ItemStack.areItemStacksEqual(_, item))
  }

  implicit class ForcedNBT(i: ItemStack) {
    def forceTag: NBTTagCompound = {
      if (!i.hasTagCompound)
        i.setTagCompound(new NBTTagCompound)
      i.getTagCompound
    }
  }

  implicit class ItemAdditions(item: Item) {
    def newStack(amt: Int = 1, damage: Int = 0, nbt: NBTTagCompound = null): ItemStack = new ItemStack(item, amt, damage, nbt)

    def newIStack(amt: Int = 1, damage: Int = 0, nbt: NBTTagCompound = null): IItemStack = Converter.IItemStackFromItemStack(newStack(amt, damage, nbt))
  }

  implicit class BlockAdditions(block: Block) {
    def newStack(amt: Int = 1, damage: Int = 0): ItemStack = new ItemStack(block, amt, damage)

    def newIStack(amt: Int = 1, damage: Int = 0): IItemStack = Converter.IItemStackFromItemStack(newStack(amt, damage))
  }

}
