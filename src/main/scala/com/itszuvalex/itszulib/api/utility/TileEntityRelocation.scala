package com.itszuvalex.itszulib.api.utility

import com.itszuvalex.itszulib.api.events.EventTileEntityRelocation
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.init.Blocks
import net.minecraft.tileentity.TileEntity
import net.minecraft.world.{World, WorldServer}
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.common.util.{BlockSnapshot, ForgeDirection}
import net.minecraftforge.event.world.BlockEvent.{BreakEvent, PlaceEvent}

/**
  * Created by Chris on 12/30/2014.
  */
object TileEntityRelocation {
  val shiftElseRemake = false

  def shiftBlock(world: World, x: Int, y: Int, z: Int, direction: ForgeDirection, player: EntityPlayer): Unit = {
    val newX = x + direction.offsetX
    val newY = y + direction.offsetY
    val newZ = z + direction.offsetZ
    moveBlock(world, x, y, z, world, newX, newY, newZ, false, player)
  }

  def moveBlock(world: World, x: Int, y: Int, z: Int, destWorld: World, destX: Int, destY: Int, destZ: Int,
                replace: Boolean = false, player: EntityPlayer): Unit = {
    if (!replace && !destWorld.isAirBlock(destX, destY, destZ)) return
    applySnapshot(extractBlock(world, x, y, z, player), destWorld, destX, destY, destZ, player)
  }

  def extractBlock(world: World, x: Int, y: Int, z: Int, player: EntityPlayer): TileSave = {
    world match {
      case world1: WorldServer =>
        if (MinecraftForge
            .EVENT_BUS
            .post(new BreakEvent(x,
                                 y,
                                 z,
                                 world,
                                 world.getBlock(x, y, z),
                                 world.getBlockMetadata(x, y, z),
                                 player))) {
          return null
        }
      case _ =>
    }
    if (MinecraftForge.EVENT_BUS.post(new EventTileEntityRelocation.Pickup(world, x, y, z))) return null
    val tileEntity = world.getTileEntity(x, y, z)
    val block = world.getBlock(x, y, z)
    val metadata = world.getBlockMetadata(x, y, z)
    val snapshot = new TileSave(world, x, y, z, block, metadata, tileEntity)
    //    world.setBlockToAir(x, y, z)
    world.removeTileEntity(x, y, z)
    //    TileContainer.shouldDrop = false
    world.setBlock(x, y, z, Blocks.air, 0, 2)
    //    TileContainer.shouldDrop = true
    snapshot
  }

  def applySnapshot(s: TileSave, player: EntityPlayer): Unit = applySnapshot(s, s.world, s.x, s.y, s.z, player): Boolean

  def applySnapshot(s: TileSave, destWorld: World, destX: Int, destY: Int, destZ: Int, player: EntityPlayer): Boolean = {
    if (s == null) return false
    if (s.block == null) return false
    if (!s.block.canPlaceBlockAt(destWorld, destX, destY, destZ)) return false
    destWorld match {
      case world1: WorldServer =>
        if (MinecraftForge
            .EVENT_BUS
            .post(new PlaceEvent(new BlockSnapshot(destWorld,
                                                   destX,
                                                   destY,
                                                   destZ,
                                                   s.block,
                                                   destWorld.getBlockMetadata(destX, destY, destZ)),
                                 destWorld.getBlock(destX, destY, destZ),
                                 player))) {
          return false
        }
      case _ =>
    }
    if (MinecraftForge.EVENT_BUS.post(new EventTileEntityRelocation.Placement(destWorld, destX, destY, destZ, s.block))) {
      return false
    }
    destWorld.setBlock(destX, destY, destZ, s.block, s.metadata, 3)
    if (s.te != null) {
      if (s.x != destX) s.te.setInteger("x", destX)
      if (s.y != destY) s.te.setInteger("y", destY)
      if (s.z != destZ) s.te.setInteger("z", destZ)
      val newTile = if (s.world == destWorld) {
        TileEntity.createAndLoadEntity(s.te)
      } else {
        val tile = s.block.createTileEntity(destWorld, s.metadata)
        tile.readFromNBT(s.te)
        tile
      }
      newTile.blockType = s.block
      destWorld.setTileEntity(destX, destY, destZ, newTile)
    }
    s.block.onBlockAdded(destWorld, destX, destY, destZ)
    s.block.onPostBlockPlaced(destWorld, destX, destY, destZ, destWorld.getBlockMetadata(destX, destY, destZ))
    true
  }
}
