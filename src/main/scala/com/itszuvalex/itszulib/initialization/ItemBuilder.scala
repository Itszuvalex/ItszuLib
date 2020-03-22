package com.itszuvalex.itszulib.initialization

import net.minecraft.creativetab.CreativeTabs
import net.minecraft.item.Item
import net.minecraft.util.ResourceLocation

import scala.collection.mutable.ArrayBuffer

class ItemBuilder[T <: Item] {
  private var itemFactory       : Option[() => T]          = None
  private var optRegistryName   : Option[ResourceLocation] = None
  private var optUnlocalizedName: Option[String]           = None
  private var optTab            : Option[CreativeTabs]     = None
  private var optHasSubtypes    : Option[Boolean]          = None
  private var optMaxDamage      : Option[Int]              = None
  private var optMaxStackSize   : Option[Int]              = None
  private var noRepair          : Boolean                  = false
  private var hasModel          : Boolean                  = true

  private var toolNoStateBuffer: ArrayBuffer[(String, Int)] = new ArrayBuffer[(String, Int)]()
  private var oresBuffer       : ArrayBuffer[String]        = new ArrayBuffer[String]()

  private var item: Option[T] = None

  def registerModel: Boolean = hasModel

  def registryName: Option[ResourceLocation] = optRegistryName

  def unlocalizedName: Option[String] = optUnlocalizedName

  def ores: Iterable[String] = oresBuffer

  def setFactory(f: () => T): ItemBuilder[T] = {itemFactory = Option(f); this}

  def setRegistryName(r: ResourceLocation): ItemBuilder[T] = {optRegistryName = Option(r); this}

  def setUnlocalizedName(s: String): ItemBuilder[T] = {optUnlocalizedName = Option(s); this}

  def setCreativeTab(t: CreativeTabs): ItemBuilder[T] = {optTab = Option(t); this}

  def addHarvestLevel(tool: String, level: Int): ItemBuilder[T] = {toolNoStateBuffer += ((tool, level)); this}

  def setNoRepair(): ItemBuilder[T] = {noRepair = true; this}

  def setHasSubtypes(b: Boolean): ItemBuilder[T] = {optHasSubtypes = Some(b); this}

  def setMaxDamage(i: Int): ItemBuilder[T] = {optMaxDamage = Some(i); this}

  def setMaxStackSize(i: Int): ItemBuilder[T] = {optMaxStackSize = Some(i); this}

  def setHasModel(b: Boolean): ItemBuilder[T] = {hasModel = b; this}

  def addOre(name: String): ItemBuilder[T] = {oresBuffer += name; this}

  def build(): T = {
    if (item.isDefined)
      return item.get

    val ret = itemFactory.get.apply()
    optRegistryName.foreach(ret.setRegistryName)
    optUnlocalizedName.foreach(ret.setUnlocalizedName)
    optTab.foreach(ret.setCreativeTab)
    optHasSubtypes.foreach(ret.setHasSubtypes)
    optMaxDamage.foreach(ret.setMaxDamage)
    optMaxStackSize.foreach(ret.setMaxStackSize)
    toolNoStateBuffer.foreach(a => ret.setHarvestLevel(a._1, a._2))
    toolNoStateBuffer.clear()

    if (noRepair) ret.setNoRepair()

    item = Some(ret)
    ret
  }

}
