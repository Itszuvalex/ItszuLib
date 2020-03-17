package com.itszuvalex.itszulib.api.wrappers

import com.itszuvalex.itszulib.api.core.ChunkCoord
import net.minecraft.block.state.IBlockState
import net.minecraft.entity.Entity
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World

class WrapperWorld(private val world: World) extends IWorld {

  override def isRemote: Boolean = world.isRemote

  override def getChunkFromChunkCoord(cc: ChunkCoord): IChunk = new WrapperChunk(world.getChunkFromChunkCoords(cc.x, cc.z))

  override def getChunkFromChunkCoords(x: Int, z: Int): IChunk = new WrapperChunk(world.getChunkFromChunkCoords(x, z))

  override def getChunkFromBlockCoords(pos: BlockPos): IChunk = new WrapperChunk(world.getChunkFromBlockCoords(pos))

  override def getTileEntity(pos: BlockPos): TileEntity = world.getTileEntity(pos)

  override def removeTileEntity(pos: BlockPos): Unit = world.removeTileEntity(pos)

  override def setTileEntity(pos: BlockPos, tile: TileEntity): Unit = world.setTileEntity(pos, tile)

  override def getBlockState(pos: BlockPos): IBlockState = world.getBlockState(pos: BlockPos)

  override def isAirBlock(pos: BlockPos): Boolean = world.isAirBlock(pos: BlockPos)

  override def setBlockState(pos: BlockPos, state: IBlockState): Boolean = world.setBlockState(pos: BlockPos, state: IBlockState)

  override def setBlockState(pos: BlockPos, state: IBlockState, flags: Int): Boolean = world.setBlockState(pos: BlockPos, state: IBlockState, flags: Int)

  override def setBlockToAir(pos: BlockPos): Boolean = world.setBlockToAir(pos: BlockPos)

  override def markAndNotifyBlock(pos: BlockPos, chunk: IChunk, blockstate: IBlockState, newstate: IBlockState, flags: Int): Unit =
    world.markAndNotifyBlock(pos, chunk.toMinecraft, blockstate, newstate, flags)

  override def markBlockRangeForRenderUpdate(min: BlockPos, max: BlockPos): Unit = world.markBlockRangeForRenderUpdate(min, max)

  override def markBlockRangeForRenderUpdate(x1: Int, y1: Int, z1: Int, x2: Int, y2: Int, z2: Int): Unit = world.markBlockRangeForRenderUpdate(x1, y1, z1, x2, y2, z2)

  override def markChunkDirty(pos: BlockPos): Unit = world.markChunkDirty(pos, null)

  override def dimensionId: Int = world.provider.getDimension

  override def isBlockLoaded(pos: BlockPos): Boolean = world.isBlockLoaded(pos)

  override def spawnEntity(entity: Entity): Boolean = world.spawnEntity(entity)

  override def toMinecraft: World = world

  override def getITileEntity(pos: BlockPos): ITileEntity = Converter.ITileEntityFromTileEntity(world.getTileEntity(pos))

  override def setITileEntity(pos: BlockPos, tile: ITileEntity): Unit = world.setTileEntity(pos, Converter.TileEntityFromITileEntity(tile))
}
