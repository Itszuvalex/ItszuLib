package com.itszuvalex.itszulib.initialization

import net.minecraft.block.Block
import net.minecraft.creativetab.CreativeTabs
import net.minecraft.item.ItemBlock
import net.minecraft.util.ResourceLocation

import scala.collection.mutable.ArrayBuffer

class BlockBuilder[T <: Block] {
  private var blockFactory   : Option[() => T]                          = None
  private var unbreakable                                               = false
  private var hardness       : Option[Float]                            = None
  private var tab            : Option[CreativeTabs]                     = None
  private var slipperiness   : Option[Float]                            = None
  private var lightLevel     : Option[Float]                            = None
  private var lightOpacity   : Option[Int]                              = None
  private var resistance     : Option[Float]                            = None
  private var unlocalizedName: Option[String]                           = None
  private var registryName   : Option[ResourceLocation]                 = None
  private var itemBlock      : Option[ItemBlockBuilder[_ <: ItemBlock]] = None

  private var toolNoState: ArrayBuffer[(String, Int)] = new ArrayBuffer[(String, Int)]()

  private var block: Option[T] = None

  def setFactory(f: () => T): BlockBuilder[T] = {blockFactory = Option(f); this}

  def setHardness(h: Float): BlockBuilder[T] = {hardness = Some(h); this}

  def setUnbreakable(): BlockBuilder[T] = {unbreakable = true; this}

  def setCreativeTab(t: CreativeTabs): BlockBuilder[T] = {tab = Option(t); this}

  def setDefaultSlipperiness(s: Float): BlockBuilder[T] = {slipperiness = Some(s); this}

  def setLightLevel(l: Float): BlockBuilder[T] = {lightLevel = Some(l); this}

  def setLightOpacity(i: Int): BlockBuilder[T] = {lightOpacity = Some(i); this}

  def setResistance(r: Float): BlockBuilder[T] = {resistance = Some(r); this}

  def setUnlocalizedName(s: String): BlockBuilder[T] = {unlocalizedName = Option(s); this}

  def setHasItemBlock[I <: ItemBlock](h: Boolean, f: Option[() => I] = None): BlockBuilder[T] = {
    if (h) {
      if (itemBlock.isEmpty) {
        val builder = new ItemBlockBuilder[I]()
          builder.setFactory(() => new ItemBlock(block.get).asInstanceOf[I])
        itemBlock = Some(builder)
        if (f.isDefined)
          builder.setFactory(f.get)
      }
    } else itemBlock = None
    this
  }

  def setHarvestLevel(tool: String, level: Int): BlockBuilder[T] = {toolNoState += ((tool, level)); this}

  def setRegistryName(name: ResourceLocation): BlockBuilder[T] = {registryName = Option(name); this}

  def hasItemBlock: Boolean = itemBlock.isDefined

  def getItemBlockBuilder[I <: ItemBlock]: ItemBlockBuilder[I] = if (!hasItemBlock) null
  else {
    itemBlock.get.asInstanceOf[ItemBlockBuilder[I]]
  }

  def build(): T = {
    if (block.isDefined)
      return block.get

    val ret = blockFactory.get.apply()
    if (unbreakable) ret.setBlockUnbreakable()
    tab.foreach(ret.setCreativeTab)
    hardness.foreach(ret.setHardness)
    slipperiness.foreach(ret.setDefaultSlipperiness)
    lightLevel.foreach(ret.setLightLevel)
    lightOpacity.foreach(ret.setLightOpacity)
    resistance.foreach(ret.setResistance)
    unlocalizedName.foreach(ret.setUnlocalizedName)
    registryName.foreach(ret.setRegistryName)
    toolNoState.foreach(a => ret.setHarvestLevel(a._1, a._2))
    toolNoState.clear()

    if (hasItemBlock) {
      unlocalizedName.foreach(getItemBlockBuilder.setUnlocalizedName)
      registryName.foreach(getItemBlockBuilder.setRegistryName)
    }

    block = Option(ret)
    ret
  }
}
