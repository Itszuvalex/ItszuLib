package com.itszuvalex.itszulib.api.utility

import com.itszuvalex.itszulib.api.events.EventTileEntityRelocation
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.init.Blocks
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.{BlockPos, EnumFacing}
import net.minecraft.world.{World, WorldServer}
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.common.util.{BlockSnapshot, ForgeDirection}
import net.minecraftforge.event.world.BlockEvent.{BreakEvent, PlaceEvent}

/**
  * Created by Chris on 12/30/2014.
  */
object TileEntityRelocation {
  val shiftElseRemake = false

  def shiftBlock(world: World, x: Int, y: Int, z: Int, direction: EnumFacing, player: EntityPlayer): Unit = {
    val newX = x + direction.getFrontOffsetX
    val newY = y + direction.getFrontOffsetY
    val newZ = z + direction.getFrontOffsetZ
    moveBlock(world, x, y, z, world, new BlockPos(newX, newY, newZ), false, player)
  }

  def moveBlock(world: World, pos: BlockPos, destWorld: World, destPos: BlockPos,
                replace: Boolean = false, player: EntityPlayer): Unit = {
    if (!replace && !destWorld.isAirBlock(destPos)) return
    applySnapshot(extractBlock(world, pos, player), destWorld, destPos, player)
  }

  def extractBlock(world: World, pos: BlockPos, player: EntityPlayer): TileSave = {
    world match {
      case world1: WorldServer =>
        if (MinecraftForge
            .EVENT_BUS
            .post(new BreakEvent(
                                 world,
              pos,
                                 world.getBlockState(pos),
                                 player))) {
          return null
        }
      case _ =>
    }
    if (MinecraftForge.EVENT_BUS.post(new EventTileEntityRelocation.Pickup(world, pos))) return null
    val tileEntity = world.getTileEntity(pos)
    val block = world.getBlock(pos)
    val metadata = world.getBlockMetadata(pos)
    val snapshot = new TileSave(world, pos, block, metadata, tileEntity)
    //    world.setBlockToAir(pos)
    world.removeTileEntity(pos)
    //    TileContainer.shouldDrop = false
    world.setBlock(pos, Blocks.air, 0, 2)
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
