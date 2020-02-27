package com.itszuvalex.itszulib.api.utility

import com.itszuvalex.itszulib.api.events.EventTileEntityRelocation
import com.itszuvalex.itszulib.api.wrappers.IWorld
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.EnumFacing
import net.minecraft.util.math.BlockPos
import net.minecraft.world.{World, WorldServer}
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.common.util.BlockSnapshot
import net.minecraftforge.event.world.BlockEvent.{BreakEvent, PlaceEvent}

/**
  * Created by Chris on 12/30/2014.
  */
object TileEntityRelocation {
  val shiftElseRemake = false

  def shiftBlock(world: IWorld, pos: BlockPos, direction: EnumFacing, player: EntityPlayer): Unit = {
    val newX = pos.getX + direction.getFrontOffsetX
    val newY = pos.getY + direction.getFrontOffsetY
    val newZ = pos.getZ + direction.getFrontOffsetZ
    moveBlock(world, pos, world, new BlockPos(newX, newY, newZ), false, player)
  }

  def moveBlock(world: IWorld, pos: BlockPos, destWorld: IWorld, destPos: BlockPos,
    replace: Boolean = false, player: EntityPlayer): Unit = {
    if (!replace && !destWorld.isAirBlock(destPos)) return
    applySnapshot(extractBlock(world, pos, player), destWorld, destPos, player)
  }

  def extractBlock(world: IWorld, pos: BlockPos, player: EntityPlayer): TileSave = {
    world.toMinecraft match {
      case world1: WorldServer =>
        if (MinecraftForge
          .EVENT_BUS
          .post(new BreakEvent(world1,
            pos,
            world.getBlockState(pos),
            player))) {
          return null
        }
      case _ =>
    }
    if (MinecraftForge.EVENT_BUS.post(new EventTileEntityRelocation.Pickup(world, pos))) return null
    val snapshot = new TileSave(world, pos)
    //    world.setBlockToAir(pos)
    world.removeTileEntity(pos)
    //    TileContainer.shouldDrop = false
    world.setBlockToAir(pos)
    //    TileContainer.shouldDrop = true
    snapshot
  }

  def applySnapshot(s: TileSave, player: EntityPlayer): Unit = applySnapshot(s, s.world, s.pos, player): Boolean

  def applySnapshot(s: TileSave, destWorld: IWorld, destPos: BlockPos, player: EntityPlayer): Boolean = {
    if (s == null) return false
    if (s.block == null) return false
    if (!s.block.canPlaceBlockAt(destWorld.toMinecraft, destPos)) return false
    destWorld.toMinecraft match {
      case world1: WorldServer =>
        if (MinecraftForge
          .EVENT_BUS
          .post(new PlaceEvent(new BlockSnapshot(world1,
            destPos,
            destWorld.getBlockState(destPos)),
            destWorld.getBlockState(destPos),
            player, player.getActiveHand))) {
          return false
        }
      case _ =>
    }
    if (MinecraftForge.EVENT_BUS.post(new EventTileEntityRelocation.Placement(destWorld, destPos, destWorld.getBlockState(destPos)))) {
      return false
    }
    destWorld.setBlockState(destPos, s.block.getStateFromMeta(s.meta))
    if (s.te != null) {
      if (s.pos.getX != destPos.getX) s.te.setInteger("x", destPos.getX)
      if (s.pos.getY != destPos.getY) s.te.setInteger("y", destPos.getY)
      if (s.pos.getZ != destPos.getZ) s.te.setInteger("z", destPos.getZ)
      val newTile = if (s.world == destWorld) {
        TileEntity.create(s.world.toMinecraft, s.te)
      } else {
        val tile = s.block.createTileEntity(destWorld.toMinecraft, s.block.getStateFromMeta(s.meta))
        tile.readFromNBT(s.te)
        tile
      }
      destWorld.setTileEntity(destPos, newTile)
    }
    s.block.onBlockAdded(destWorld.toMinecraft, destPos, s.block.getStateFromMeta(s.meta))
    true
  }
}
