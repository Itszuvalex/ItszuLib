package com.itszuvalex.itszulib.initialization

import net.minecraft.creativetab.CreativeTabs
import net.minecraft.item.Item
import net.minecraft.util.ResourceLocation

import scala.collection.mutable.ArrayBuffer

class ItemBuilder[T <: Item] {
  private var itemFactory    : Option[() => T]          = None
  private var registryName   : Option[ResourceLocation] = None
  private var unlocalizedName: Option[String]           = None
  private var tab            : Option[CreativeTabs]     = None
  private var hasSubtypes    : Option[Boolean]          = None
  private var maxDamage      : Option[Int]              = None
  private var maxStackSize   : Option[Int]              = None
  private var noRepair       : Boolean                  = false

  private var toolNoState: ArrayBuffer[(String, Int)] = new ArrayBuffer[(String, Int)]()

  private var item: Option[T] = None

  def setFactory(f: () => T) = {itemFactory = Option(f); this}

  def setRegistryName(r: ResourceLocation) = {registryName = Option(r); this}

  def setUnlocalizedName(s: String) = {unlocalizedName = Option(s); this}

  def setCreativeTab(t: CreativeTabs) = {tab = Option(t); this}

  def setHarvestLevel(tool: String, level: Int) = {toolNoState += ((tool, level)); this}

  def setNoRepair() = {noRepair = true; this}

  def setHasSubtypes(b: Boolean) = {hasSubtypes = Some(b); this}

  def setMaxDamage(i: Int) = {maxDamage = Some(i); this}

  def setMaxStackSize(i: Int) = {maxStackSize = Some(i); this}

  def build(): T = {
    if (item.isDefined)
      return item.get

    val ret = itemFactory.get.apply()
    registryName.foreach(ret.setRegistryName)
    unlocalizedName.foreach(ret.setUnlocalizedName)
    tab.foreach(ret.setCreativeTab)
    hasSubtypes.foreach(ret.setHasSubtypes)
    maxDamage.foreach(ret.setMaxDamage)
    maxStackSize.foreach(ret.setMaxStackSize)
    toolNoState.foreach(a => ret.setHarvestLevel(a._1, a._2))
    toolNoState.clear()

    if (noRepair) ret.setNoRepair()

    item = Some(ret)
    ret
  }

}
