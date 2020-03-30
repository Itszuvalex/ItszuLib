package com.itszuvalex.itszulib.util

import com.itszuvalex.itszulib.api.ItszuLibModules
import com.itszuvalex.itszulib.api.core.Loc4
import com.itszuvalex.itszulib.api.storage.{IFluidStorage, IItemStorage}
import com.itszuvalex.itszulib.api.utility.FacingUtil
import com.itszuvalex.itszulib.api.wrappers.{Converter, ITileEntity}
import com.itszuvalex.itszulib.core.{EnumAutomaticIO, SidedFluidStorageConfiguration, SidedItemStorageConfiguration}
import net.minecraft.util.EnumFacing

object TileEntityUtils {
  def getIItemStorageFromTileEntity(te: ITileEntity, facing: EnumFacing): Option[IItemStorage] = {
    te match {
      case _ if te.hasModule(ItszuLibModules.ITEM_STORAGE, facing) => te.moduleOption(ItszuLibModules.ITEM_STORAGE, facing)
      case _ if te.hasModule(ItszuLibModules.ITEM_MINECRAFT_INVENTORY, facing) => Some(Converter.IItemStorageFromIItemHandler(te.getModule(ItszuLibModules.ITEM_MINECRAFT_INVENTORY, facing)))
      case _ => None
    }
  }

  def incrementTicks(ticks: Int, ticksToAct: Int): Int = {
    if (ticksToAct <= 0) 0 else
      (ticks - 1 + ticksToAct) % ticksToAct
  }

  def checkDoItemInputIO(te: ITileEntity, config: SidedItemStorageConfiguration, ticks: Int, inputSize: Int): Unit = {
    if (ticks != 0) return

    var isize = inputSize
    TileEntityUtils.getItemStoragesForIO(te, config, EnumAutomaticIO.INPUT).exists { pair =>
      isize = pair._1.transferIntoStorage(pair._2, isize)
      isize <= 0
    }
  }

  /**
    *
    * @param io
    * @return Iterable Pairs of Storages.  First is outside, second is our te's.
    */
  def getItemStoragesForIO(te: ITileEntity, sidedStorageConfig: SidedItemStorageConfiguration, io: EnumAutomaticIO): Iterable[(IItemStorage, IItemStorage)] = {
    val facings = sidedStorageConfig.automaticIO.zipWithIndex.filter(_._1 == io).map(a => FacingUtil.getAbsoluteFacingFromHorizontalRelative(EnumFacing.VALUES(a._2), sidedStorageConfig.front())).map(a => (new Loc4(te).getOffset(a), a))
    val tiles   = facings.map(pair => (pair._1.getITileEntity(force = false).orNull, pair._2)).filterNot(_._1 == null)
    tiles.map { pair =>
      val inputStorage = if (pair._1.hasModule(ItszuLibModules.ITEM_STORAGE, pair._2.getOpposite)) pair._1.getModule(ItszuLibModules.ITEM_STORAGE, pair._2.getOpposite)
      else if (pair._1.hasModule(ItszuLibModules.ITEM_MINECRAFT_INVENTORY, pair._2.getOpposite)) Converter.IItemStorageFromIItemHandler(pair._1.getModule(ItszuLibModules.ITEM_MINECRAFT_INVENTORY, pair._2.getOpposite))
      else null
      (inputStorage, sidedStorageConfig.getStorageForGlobalFacing(pair._2))
    }.filterNot(_._1 == null).filterNot(_._2 == null)
  }

  def checkDoItemOutputIO(te: ITileEntity, config: SidedItemStorageConfiguration, ticks: Int, outputSize: Int): Unit = {
    if (ticks != 0) return

    var osize = outputSize
    TileEntityUtils.getItemStoragesForIO(te, config, EnumAutomaticIO.OUTPUT).exists { pair =>
      osize = pair._2.transferIntoStorage(pair._1, osize)
      osize <= 0
    }
  }

  def checkDoFluidInputIO(te: ITileEntity, config: SidedFluidStorageConfiguration, ticks: Int, inputSize: Int): Unit = {
    if (ticks != 0) return

    var isize = inputSize

    /*
    //Match first
    TileEntityUtils.getFluidStoragesForIO(te, config, EnumAutomaticIO.INPUT).exists { pair =>
      pair._1.getTankProperties.filterNot(_.getContents == null).exists { prop =>
        val fluid         = prop.getContents
        val room          = prop.getCapacity - fluid.amount
        val toDrain       = Math.min(room, isize)
        val fluidToRemove = new FluidStack(fluid.getFluid, toDrain)
        val amtFilled     = pair._2.fill(pair._1.drain(fluidToRemove, false), true)
        pair._1.drain(amtFilled, true)
        isize -= amtFilled
        isize <= 0
      }
      isize <= 0
    }
     */

    // Generic drain
    TileEntityUtils.getFluidStoragesForIO(te, config, EnumAutomaticIO.INPUT).exists { pair =>
      val amtFilled = pair._2.fill(pair._1.drainIStack(isize, false), true)
      pair._1.drainIStack(amtFilled, true)
      isize -= amtFilled
      isize <= 0
    }
  }

  def getFluidStoragesForIO(te: ITileEntity, sidedStorageConfig: SidedFluidStorageConfiguration, io: EnumAutomaticIO): Iterable[(IFluidStorage, IFluidStorage)] = {
    val facings = sidedStorageConfig.automaticIO.zipWithIndex.filter(_._1 == io).map(a => FacingUtil.getAbsoluteFacingFromHorizontalRelative(EnumFacing.VALUES(a._2), sidedStorageConfig.front())).map(a => (new Loc4(te).getOffset(a), a))
    val tiles   = facings.map(pair => (pair._1.getITileEntity(force = false).orNull, pair._2)).filterNot(_._1 == null)
    tiles.map { pair =>
      val inputStorage = if (pair._1.hasModule(ItszuLibModules.FLUID_STORAGE, pair._2.getOpposite)) pair._1.getModule(ItszuLibModules.FLUID_STORAGE, pair._2.getOpposite)
      else if (pair._1.hasModule(ItszuLibModules.FLUID_MINECRAFT_HANDLER, pair._2.getOpposite)) Converter.IFluidStorageFromIFluidHandler(pair._1.getModule(ItszuLibModules.FLUID_MINECRAFT_HANDLER, pair._2.getOpposite))
      else null
      (inputStorage, sidedStorageConfig.getStorageForGlobalFacing(pair._2))
    }.filterNot(_._1 == null).filterNot(_._2 == null)
  }

  def checkDoFluidOutputIO(te: ITileEntity, config: SidedFluidStorageConfiguration, ticks: Int, inputSize: Int): Unit = {
    if (ticks != 0) return

    var isize = inputSize

    /*
    //Match first
    TileEntityUtils.getFluidStoragesForIO(te, config, EnumAutomaticIO.OUTPUT).exists { pair =>
      pair._2.getTankProperties.filterNot(_.getContents == null).exists { prop =>
        val fluid         = prop.getContents
        val room          = prop.getCapacity - fluid.amount
        val toDrain       = Math.min(room, isize)
        val fluidToRemove = new FluidStack(fluid.getFluid, toDrain)
        val amtFilled     = pair._1.fill(pair._2.drain(fluidToRemove, false), true)
        pair._2.drain(amtFilled, true)
        isize -= amtFilled
        isize <= 0
      }
      isize <= 0
    }
     */

    if (isize > 0) {
      // Generic drain
      TileEntityUtils.getFluidStoragesForIO(te, config, EnumAutomaticIO.OUTPUT).exists { pair =>
        val amtFilled = pair._1.fill(pair._2.drainIStack(isize, false), true)
        pair._2.drainIStack(amtFilled, true)
        isize -= amtFilled
        isize <= 0
      }
    }
  }
}
