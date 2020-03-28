package com.itszuvalex.itszulib.util

import java.util.regex.{Matcher, Pattern}

import com.itszuvalex.itszulib.ItszuLib
import net.minecraft.block.Block
import net.minecraft.item.{Item, ItemStack}
import net.minecraft.util.ResourceLocation
import org.apache.logging.log4j.Level

/**
  * Created by Chris on 9/15/2014.
  */
object StringUtils {
  val itemModIDGroup            = "modID"
  val itemNameGroup             = "itemName"
  val itemIDGroup               = "itemID"
  val itemDamageGroup           = "itemDamage"
  val itemStackSizeGroup        = "itemStackSize"
  val itemIDRegex     : String  = "(?<" + itemIDGroup + ">\\d+)"
  val modIDRegex      : String  = "(?<" + itemModIDGroup + ">[^:-]+)"
  val itemNameRegex   : String  = "(?<" + itemNameGroup + ">[^:-]+)"
  val itemStackRegex  : String  = "(?:-(?<" + itemStackSizeGroup + ">\\d+?))?"
  val itemDamageRegex : String  = "(?::(?<" + itemDamageGroup + ">\\d+?))?"
  val itemStackPattern: Pattern = Pattern
    .compile("(?:" + itemIDRegex + "|(?:" + modIDRegex + ":" + itemNameRegex + "))" + itemDamageRegex + itemStackRegex)

  def itemStackFromString(s: String): ItemStack = {
    if (s == null || s.isEmpty) return null
    val itemMatcher: Matcher = itemStackPattern.matcher(s)
    if (itemMatcher.matches) {
      try {
        val itemID    = itemMatcher.group(itemIDGroup)
        val modID     = itemMatcher.group(itemModIDGroup)
        val name      = itemMatcher.group(itemNameGroup)
        val sdam      = itemMatcher.group(itemDamageGroup)
        val ssize     = itemMatcher.group(itemStackSizeGroup)
        val damage    = if (sdam == null) 0 else sdam.toInt
        val stackSize = if (ssize == null) 1 else ssize.toInt
        if (itemID != null) {
          val id = itemID.toInt
          return new ItemStack(Item.getItemById(id), stackSize, damage)
        }
        val item = Block.REGISTRY.getObject(new ResourceLocation(modID, name))
        if (item != null) {
          return new ItemStack(item, stackSize, damage)
        }
        val block = Block.REGISTRY.getObject(new ResourceLocation(modID, name))
        if (block != null) {
          return new ItemStack(block, stackSize, damage)
        }
      }
      catch {
        case e: Exception =>
          ItszuLib.logger
                  .log(Level.ERROR, "Error parsing ItemStack string \"" + s + "\"")
          e.printStackTrace()
          return null
      }
      ItszuLib.logger.log(Level.ERROR, "Error parsing ItemStack string \"" + s + "\"")
    }
    null
  }

  def itemStackToString(s: ItemStack): String = {
    //    var id: GameRegistry.ItemStackHolder = null
    //    if (s != null) {
    //      if (s.getItem.isInstanceOf[ItemBlock]) {
    //        id = GameRegistry.findUniqueIdentifierFor(Block.getBlockFromItem(s.getItem))
    //      } else {
    //        id = GameRegistry.findUniqueIdentifierFor(s.getItem)
    //      }
    //    }
    //    if (s == null) {
    //      ""
    //    } else {
    //      var result: String = null
    //      if (id == null) {
    //        result = String.valueOf(Item.getIdFromItem(s.getItem))
    //      } else {
    //        result = id.modId + ":" + id.name
    //      }
    //      result + ":" + s.getItemDamage + "-" + s.stackSize
    null
  }

  /**
    *
    * @param input
    * @return input with first letter capitalized.
    */
  def capitalize(input: String): String = input.substring(0, 1).toUpperCase + input.substring(1)
}

