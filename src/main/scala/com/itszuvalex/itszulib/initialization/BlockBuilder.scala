package com.itszuvalex.itszulib.initialization

import net.minecraft.block.Block
import net.minecraft.creativetab.CreativeTabs
import net.minecraft.item.ItemBlock
import net.minecraft.util.ResourceLocation

import scala.collection.mutable.ArrayBuffer

class BlockBuilder[T <: Block] {
  private var blockFactory      : Option[() => T]                          = None
  private var unbreakable                                                  = false
  private var optHardness       : Option[Float]                            = None
  private var optTab            : Option[CreativeTabs]                     = None
  private var optSlipperiness   : Option[Float]                            = None
  private var optLightLevel     : Option[Float]                            = None
  private var optLightOpacity   : Option[Int]                              = None
  private var optResistance     : Option[Float]                            = None
  private var optUnlocalizedName: Option[String]                           = None
  private var optRegistryName   : Option[ResourceLocation]                 = None
  private var optItemBlock      : Option[ItemBlockBuilder[_ <: ItemBlock]] = None
  private var hasModel          : Boolean                                  = true

  private var toolNoState: ArrayBuffer[(String, Int)] = new ArrayBuffer[(String, Int)]()
  private var oresBuffer : ArrayBuffer[String]        = new ArrayBuffer[String]()

  private var block: Option[T] = None

  def registerModel: Boolean = hasModel

  def registryName: Option[ResourceLocation] = optRegistryName

  def unlocalizedName: Option[String] = optUnlocalizedName

  def ores: Iterable[String] = oresBuffer

  def setFactory(f: () => T): BlockBuilder[T] = {blockFactory = Option(f); this}

  def setHardness(h: Float): BlockBuilder[T] = {optHardness = Some(h); this}

  def setUnbreakable(): BlockBuilder[T] = {unbreakable = true; this}

  def setCreativeTab(t: CreativeTabs): BlockBuilder[T] = {optTab = Option(t); this}

  def setDefaultSlipperiness(s: Float): BlockBuilder[T] = {optSlipperiness = Some(s); this}

  def setLightLevel(l: Float): BlockBuilder[T] = {optLightLevel = Some(l); this}

  def setLightOpacity(i: Int): BlockBuilder[T] = {optLightOpacity = Some(i); this}

  def setResistance(r: Float): BlockBuilder[T] = {optResistance = Some(r); this}

  def setUnlocalizedName(s: String): BlockBuilder[T] = {optUnlocalizedName = Option(s); this}

  def setHasItemBlock[I <: ItemBlock](h: Boolean, f: Option[() => I] = None): BlockBuilder[T] = {
    if (h) {
      if (optItemBlock.isEmpty) {
        val builder = new ItemBlockBuilder[I]()
        builder.setFactory(() => new ItemBlock(block.get).asInstanceOf[I])
        optItemBlock = Some(builder)
        if (f.isDefined)
          builder.setFactory(f.get)
      }
    } else optItemBlock = None
    this
  }

  def setRegisterModel(b: Boolean): BlockBuilder[T] = {hasModel = b; this}

  def addHarvestLevel(tool: String, level: Int): BlockBuilder[T] = {toolNoState += ((tool, level)); this}

  def addOre(name: String): BlockBuilder[T] = {oresBuffer += name; this}

  def setRegistryName(name: ResourceLocation): BlockBuilder[T] = {optRegistryName = Option(name); this}

  def hasItemBlock: Boolean = optItemBlock.isDefined

  def getItemBlockBuilder[I <: ItemBlock]: ItemBlockBuilder[I] = if (!hasItemBlock) null
  else {
    optItemBlock.get.asInstanceOf[ItemBlockBuilder[I]]
  }

  def build(): T = {
    if (block.isDefined)
      return block.get

    val ret = blockFactory.get.apply()
    if (unbreakable) ret.setBlockUnbreakable()
    optTab.foreach(ret.setCreativeTab)
    optHardness.foreach(ret.setHardness)
    optSlipperiness.foreach(ret.setDefaultSlipperiness)
    optLightLevel.foreach(ret.setLightLevel)
    optLightOpacity.foreach(ret.setLightOpacity)
    optResistance.foreach(ret.setResistance)
    optUnlocalizedName.foreach(ret.setUnlocalizedName)
    optRegistryName.foreach(ret.setRegistryName)
    toolNoState.foreach(a => ret.setHarvestLevel(a._1, a._2))
    toolNoState.clear()

    if (hasItemBlock) {
      optUnlocalizedName.foreach(getItemBlockBuilder.setUnlocalizedName)
      optRegistryName.foreach(getItemBlockBuilder.setRegistryName)
    }

    block = Option(ret)
    ret
  }
}
