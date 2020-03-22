package com.itszuvalex.itszulib.initialization

import com.itszuvalex.itszulib.ItszuLib
import com.itszuvalex.itszulib.util.Debug
import net.minecraft.block.Block
import net.minecraft.item.{Item, ItemBlock}
import net.minecraftforge.event.RegistryEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.oredict.OreDictionary

import scala.collection.mutable.ArrayBuffer

class InitializationManager {
  val initializationStages =
    Map(
      InitializationStage.Pre -> new ArrayBuffer[() => Unit](),
      InitializationStage.Main -> new ArrayBuffer[() => Unit](),
      InitializationStage.Post -> new ArrayBuffer[() => Unit]()
      )
  val registryBlock        = new ArrayBuffer[RegistryEvent.Register[Block] => Unit]()
  val registryItem         = new ArrayBuffer[RegistryEvent.Register[Item] => Unit]()

  val modelRegistriesBlock = new ArrayBuffer[BlockBuilder[_ <: Block]]()
  val modelRegistriesItem  = new ArrayBuffer[ItemBuilder[_ <: Item]]()

  val oreRegistriesBlock = new ArrayBuffer[BlockBuilder[_ <: Block]]()
  val oreRegistriesItem  = new ArrayBuffer[ItemBuilder[_ <: Item]]()

  var currentStage = InitializationStage.Pre

  def addInitStage(stage: InitializationStage, run: () => Unit): Unit = {
    if (stage == InitializationStage.Complete)
      throw new IllegalArgumentException(s"Cannot add to Complete stage.")
    initializationStages(stage) += run
  }

  def runInitStage(stage: InitializationStage): Unit = {
    Debug.assert(currentStage == stage, "We should run stages in order and not skip any")
    initializationStages(stage).foreach(_ ())
    initializationStages(stage).clear()
    currentStage = currentStage.Next()
  }

  def addInitializable(i: IInitializable): Unit = {
    addInitStage(InitializationStage.Pre, () => i.preInit())
    addInitStage(InitializationStage.Main, () => i.init())
    addInitStage(InitializationStage.Post, () => i.postInit())
  }

  def addBlockRegistry(run: RegistryEvent.Register[Block] => Unit): Unit = {
    registryBlock += run
  }

  def addItemRegistry(run: RegistryEvent.Register[Item] => Unit): Unit = {
    registryItem += run
  }

  def addBlockBuilder[T <: Block](builder: BlockBuilder[T], setter: T => Unit): Unit = {
    addBlockRegistry(r => {
      val b = builder.build()
      r.getRegistry.register(b)
      setter(b)
    })
    if (builder.hasItemBlock) {
      addItemBuilder[ItemBlock](builder.getItemBlockBuilder[ItemBlock], _ => {})
    }
    if (builder.ores.nonEmpty)
      addBlockOre(builder)
  }

  def addItemBuilder[I <: Item](builder: ItemBuilder[I], setter: I => Unit): Unit = {
    addItemRegistry(r => {
      val i = builder.build()
      r.getRegistry.register(i)
      setter(i)
    })
    if (builder.ores.nonEmpty)
      addItemOre(builder)
  }

  def addBlockModel[T <: Block](builder: BlockBuilder[T]): Unit = {
    if (modelRegistriesBlock.isEmpty)
      addInitStage(InitializationStage.Main, () => {
        modelRegistriesBlock.foreach(b => ItszuLib.proxy.registerBlockModel(b))
        modelRegistriesBlock.clear()
      })
    modelRegistriesBlock += builder
  }

  def addItemModel[I <: Item](builder: ItemBuilder[I]): Unit = {
    if (modelRegistriesItem.isEmpty)
      addInitStage(InitializationStage.Main, () => {
        modelRegistriesItem.foreach(b => ItszuLib.proxy.registerItemModel(b))
        modelRegistriesItem.clear()
      })
    modelRegistriesItem += builder
  }

  def addBlockOre[T <: Block](builder: BlockBuilder[T]): Unit = {
    if (oreRegistriesBlock.isEmpty)
      addInitStage(InitializationStage.Main, () => {
        oreRegistriesBlock.foreach(b => b.ores.foreach(n => OreDictionary.registerOre(n, b.build())))
        oreRegistriesBlock.clear()
      })
    oreRegistriesBlock += builder
  }

  def addItemOre[T <: Item](builder: ItemBuilder[T]): Unit = {
    if (oreRegistriesItem.isEmpty)
      addInitStage(InitializationStage.Main, () => {
        oreRegistriesItem.foreach(b => b.ores.foreach(n => OreDictionary.registerOre(n, b.build())))
        oreRegistriesItem.clear()
      })
    oreRegistriesItem += builder
  }

  @SubscribeEvent
  def registerBlocks(event: RegistryEvent.Register[Block]): Unit = {
    registryBlock.foreach(_ (event))
  }

  @SubscribeEvent
  def registerItems(event: RegistryEvent.Register[Item]): Unit = {
    registryItem.foreach(_ (event))
  }

}
