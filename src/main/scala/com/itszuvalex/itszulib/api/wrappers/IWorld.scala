package com.itszuvalex.itszulib.api.wrappers

import com.itszuvalex.itszulib.api.core.ChunkCoord
import net.minecraft.block.state.IBlockState
import net.minecraft.entity.Entity
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World

trait IWorld {

  def getChunkFromChunkCoord(cc: ChunkCoord): IChunk

  def getChunkFromChunkCoords(x: Int, z: Int): IChunk

  def getChunkFromBlockCoords(pos: BlockPos): IChunk

  def getTileEntity(pos: BlockPos): TileEntity

  def getITileEntity(pos: BlockPos): ITileEntity

  def getBlockState(pos: BlockPos): IBlockState

  def isAirBlock(pos: BlockPos): Boolean

  def setBlockState(pos: BlockPos, state: IBlockState): Boolean

  def setBlockState(pos: BlockPos, state: IBlockState, flags: Int): Boolean

  def setBlockToAir(pos: BlockPos): Boolean

  def markAndNotifyBlock(pos: BlockPos, chunk: IChunk, blockstate: IBlockState, newstate: IBlockState, flags: Int): Unit

  def markBlockRangeForRenderUpdate(min: BlockPos, max: BlockPos): Unit

  def markBlockRangeForRenderUpdate(x1: Int, y1: Int, z1: Int, x2: Int, y2: Int, z2: Int): Unit

  def markChunkDirty(pos: BlockPos): Unit

  def dimensionId: Int

  def toMinecraft: World

  def isBlockLoaded(pos: BlockPos): Boolean

  def spawnEntity(entity: Entity): Boolean

  def removeTileEntity(pos: BlockPos): Unit

  def setTileEntity(pos: BlockPos, tile: TileEntity): Unit

  def setITileEntity(pos: BlockPos, tile: ITileEntity): Unit
}
