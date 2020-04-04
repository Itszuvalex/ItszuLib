package com.itszuvalex.itszulib

import com.itszuvalex.itszulib.api.core.{ChunkCoord, Loc4}
import com.itszuvalex.itszulib.api.wrappers._
import net.minecraft.block.state.IBlockState
import net.minecraft.entity.Entity
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.EnumFacing
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World

import scala.collection.mutable

class TestableWorld(private val dim: Int) extends IWorld {
  private val tileMap  = new mutable.HashMap[BlockPos, ITileEntity]()
  private val chunkMap = new mutable.HashMap[ChunkCoord, IChunk]()

  TestBase.testableWorlds(dim) = this

  override def isRemote: Boolean = false

  override def getChunkFromChunkCoord(cc: ChunkCoord): IChunk = chunkMap.getOrElseUpdate(cc, new TestableChunk(this, cc))

  override def getChunkFromChunkCoords(x: Int, z: Int): IChunk = getChunkFromChunkCoord(ChunkCoord(x, z))

  override def getChunkFromBlockCoords(pos: BlockPos): IChunk = getChunkFromChunkCoord(ChunkCoord(pos))

  override def getTileEntity(pos: BlockPos): TileEntity = tileMap.get(pos).map(_.toMinecraft).orNull

  override def getBlockState(pos: BlockPos): IBlockState = null

  override def isAirBlock(pos: BlockPos): Boolean = ???

  override def setBlockState(pos: BlockPos, state: IBlockState): Boolean = ???

  override def setBlockState(pos: BlockPos, state: IBlockState, flags: Int): Boolean = ???

  override def setBlockToAir(pos: BlockPos): Boolean = ???

  override def markAndNotifyBlock(pos: BlockPos, chunk: IChunk, blockstate: IBlockState, newstate: IBlockState, flags: Int): Unit = {}

  override def markBlockRangeForRenderUpdate(min: BlockPos, max: BlockPos): Unit = {}

  override def markBlockRangeForRenderUpdate(x1: Int, y1: Int, z1: Int, x2: Int, y2: Int, z2: Int): Unit = {}

  override def markChunkDirty(pos: BlockPos): Unit = {}

  override def dimensionId: Int = dim

  override def toMinecraft: World = null

  override def isBlockLoaded(pos: BlockPos): Boolean = true

  override def spawnEntity(entity: Entity): Boolean = ???

  override def removeTileEntity(pos: BlockPos): Unit = tileMap.remove(pos)

  override def setTileEntity(pos: BlockPos, tile: TileEntity): Unit = tileMap(pos) = Converter.ITileEntityFromTileEntity(tile)

  override def getITileEntity(pos: BlockPos): ITileEntity = tileMap.get(pos).orNull

  override def setITileEntity(pos: BlockPos, tile: ITileEntity): Unit = tileMap(pos) = tile

  override def notifyBlockUpdate(getPos: BlockPos, state: IBlockState, state1: IBlockState, i: Int): Unit = {}

  override def notifyNeighborsOfStateChange(getPos: BlockPos, getBlock: IBlock, bool: Boolean): Unit = {
    EnumFacing.VALUES.map(Loc4(this.asInstanceOf[IWorld], getPos).getOffset(_)).foreach { nloc =>
      nloc.getITileEntity() match {
        case None =>
        case Some(t: IBlockCallbacks) =>
          t.onNeighborChanged(this, nloc.getPos, getBlockState(nloc.getPos), getBlock, getPos)
      }
    }
  }
}
