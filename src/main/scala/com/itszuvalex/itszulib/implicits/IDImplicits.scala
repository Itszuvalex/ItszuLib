package com.itszuvalex.itszulib.implicits

import net.minecraft.block.Block
import net.minecraft.item.{Item, ItemStack}

/**
  * Created by Christopher Harris (Itszuvalex) on 10/19/14.
  */
object IDImplicits {

  implicit class ItemIDImplicits(i: Item) {
    def itemID: Int = Item.getIdFromItem(i)

    def getBlock: Block = Block.getBlockFromItem(i)
  }

  implicit class BlockIDImplicits(b: Block) {
    def getItem: Item = Item.getItemFromBlock(b)

    def blockID: Int = Block.getIdFromBlock(b)

  }

  implicit class ItemStackImplicits(i: ItemStack) {
    def itemID: Int = Item.getIdFromItem(i.getItem)
  }

  implicit class IntegerIDImplicits(i: Int) {
    def getItem: Item = Item.getItemById(i)

    def getBlock: Block = Block.getBlockById(i)
  }

  implicit class StringIDImplicits(s: String) {
    def getBlock: Block = Block.getBlockFromName(s)
  }

}
