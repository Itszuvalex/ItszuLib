package com.itszuvalex.itszulib.api

import net.minecraft.init.{Blocks, Items}
import net.minecraft.item._
import net.minecraft.util.EnumFacing
import net.minecraftforge.common.capabilities.{Capability, ICapabilityProvider}
import net.minecraftforge.event.ForgeEventFactory

/**
  * Created by Chris on 1/10/2017.
  */
object IBurnable {
  def getProvider(stack: ItemStack): Option[ICapabilityProvider] = {
    if ( stack == null) return None
    if (stack.isEmpty) return None

    // Shamelessly taken from TileEntityFurnace
    val burn = stack.getItem match {
      case null => -1
      case i: ItemTool if "WOOD" == i.getToolMaterialName => 200
      case i: ItemSword if "WOOD" == i.getToolMaterialName => 200
      case i: ItemHoe if "WOOD" == i.getMaterialName => 200
      case i if i == Items.STICK => 100
      case i if i == Items.COAL => 1600
      case i if i == Items.LAVA_BUCKET => 20000
      case i if i == Item.getItemFromBlock(Blocks.SAPLING) => 100
      case i if i == Items.BLAZE_ROD => 2400
      case _ => ForgeEventFactory.getItemBurnTime(stack)
    }

    if (burn <= 0) None
    else Some(
      new ICapabilityProvider {
        override def getCapability[T](capability: Capability[T], facing: EnumFacing): T = if (capability == Capabilities.ITEM_BURNABLE)
          new IBurnable {
            override def getBurnTime: Int = burn
          }.asInstanceOf[T]
        else null.asInstanceOf[T]

        override def hasCapability(capability: Capability[_], facing: EnumFacing): Boolean = capability == Capabilities.ITEM_BURNABLE
      }
    )
  }
}

trait IBurnable {
  def getBurnTime: Int
}
