package com.itszuvalex.itszulib.core.traits.tile

import net.minecraft.client.renderer.Tessellator
import net.minecraft.inventory.ISidedInventory
import net.minecraft.item.ItemStack
import net.minecraft.util.IIcon

/**
  * Created by Alex on 02.03.2016.
  */
object SidedMultiBlockComponent {

  def renderSideOverlay(x: Double, y: Double, z: Double, side: Int, icon: IIcon): Unit = {
    val tess = Tessellator.instance
    tess.startDrawingQuads()
    side match {
      case 0 =>
        tess.addVertexWithUV(x, y - .001, z + 1, icon.getMinU, icon.getMinV)
        tess.addVertexWithUV(x, y - .001, z, icon.getMaxU, icon.getMinV)
        tess.addVertexWithUV(x + 1, y - .001, z, icon.getMaxU, icon.getMaxV)
        tess.addVertexWithUV(x + 1, y - .001, z + 1, icon.getMinU, icon.getMaxV)
      case 1 =>
        tess.addVertexWithUV(x, y + .001, z, icon.getMinU, icon.getMinV)
        tess.addVertexWithUV(x, y + .001, z + 1, icon.getMaxU, icon.getMinV)
        tess.addVertexWithUV(x + 1, y + .001, z + 1, icon.getMaxU, icon.getMaxV)
        tess.addVertexWithUV(x + 1, y + .001, z, icon.getMinU, icon.getMaxV)
      case 2 =>
        tess.addVertexWithUV(x + 1, y, z - .001, icon.getMinU, icon.getMinV)
        tess.addVertexWithUV(x, y, z - .001, icon.getMaxU, icon.getMinV)
        tess.addVertexWithUV(x, y + 1, z - .001, icon.getMaxU, icon.getMaxV)
        tess.addVertexWithUV(x + 1, y + 1, z - .001, icon.getMinU, icon.getMaxV)
      case 3 =>
        tess.addVertexWithUV(x, y, z + .001, icon.getMinU, icon.getMinV)
        tess.addVertexWithUV(x + 1, y, z + .001, icon.getMaxU, icon.getMinV)
        tess.addVertexWithUV(x + 1, y + 1, z + .001, icon.getMaxU, icon.getMaxV)
        tess.addVertexWithUV(x, y + 1, z + .001, icon.getMinU, icon.getMaxV)
      case 4 =>
        tess.addVertexWithUV(x - .001, y, z, icon.getMinU, icon.getMinV)
        tess.addVertexWithUV(x - .001, y, z + 1, icon.getMaxU, icon.getMinV)
        tess.addVertexWithUV(x - .001, y + 1, z + 1, icon.getMaxU, icon.getMaxV)
        tess.addVertexWithUV(x - .001, y + 1, z, icon.getMinU, icon.getMaxV)
      case 5 =>
        tess.addVertexWithUV(x - .001, y, z + 1, icon.getMinU, icon.getMinV)
        tess.addVertexWithUV(x - .001, y, z, icon.getMaxU, icon.getMinV)
        tess.addVertexWithUV(x - .001, y + 1, z, icon.getMaxU, icon.getMaxV)
        tess.addVertexWithUV(x - .001, y + 1, z + 1, icon.getMinU, icon.getMaxV)
    }
    tess.draw()
  }

}

trait SidedMultiBlockComponent extends MultiBlockComponent with ISidedInventory {

  val groups: Array[Array[Int]]
  var sideConfig = Array[Int](-1, -1, -1, -1, -1, -1)

  override def getAccessibleSlotsFromSide(side: Int): Array[Int] = {
    if (groups.isDefinedAt(sideConfig(side))) {
      groups(sideConfig(side))
    } else {
      Array[Int]()
    }
  }

  override def canInsertItem(slot: Int, item: ItemStack, side: Int): Boolean = {
    if (groups.isDefinedAt(sideConfig(side))) {
      canInsertItemIntoGroup(slot, item, sideConfig(side))
    } else {
      false
    }
  }

  override def canExtractItem(slot: Int, item: ItemStack, side: Int): Boolean = {
    if (groups.isDefinedAt(sideConfig(side))) {
      canExtractItemFromGroup(slot, item, sideConfig(side))
    } else {
      false
    }
  }

  def canInsertItemIntoGroup(slot: Int, item: ItemStack, group: Int): Boolean

  def canExtractItemFromGroup(slot: Int, item: ItemStack, group: Int): Boolean

  def shiftConfig(side: Int): Unit = {
    if (groups.isDefinedAt(sideConfig(side) + 1)) {
      sideConfig(side) += 1
    } else {
      sideConfig(side) = -1
    }
  }

}
