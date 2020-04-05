/*
 * ******************************************************************************
 *  * Copyright (C) 2013  Christopher Harris (Itszuvalex)
 *  * Itszuvalex@gmail.com
 *  *
 *  * This program is free software; you can redistribute it and/or
 *  * modify it under the terms of the GNU General Public License
 *  * as published by the Free Software Foundation; either version 2
 *  * of the License, or (at your option) any later version.
 *  *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  * GNU General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * along with this program; if not, write to the Free Software
 *  * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 *  *****************************************************************************
 */
package com.itszuvalex.itszulib.api.core

import com.itszuvalex.itszulib.ItszuLib
import com.itszuvalex.itszulib.api.wrappers._
import net.minecraft.block.Block
import net.minecraft.block.state.IBlockState
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.EnumFacing
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import net.minecraftforge.common.util.INBTSerializable

/**
  * Created by Christopher Harris (Itszuvalex) on 5/9/14.
  */
object Loc4 {

  val VanillaDimensionMapper: DimensionMapper = new DimensionMapper {
    override def dimensionIdForWorld(w: IWorld): Int = w.dimensionId

    override def worldForDimensionId(i: Int): IWorld = ItszuLib.proxy.getIWorld(i)
  }

  var OverrideDimensionMapper: Option[DimensionMapper] = None

  def DimensionMapper: DimensionMapper = OverrideDimensionMapper.getOrElse(VanillaDimensionMapper)

  val ORIGIN: Loc4 = Loc4Indirect(0, new BlockPos(0, 0, 0))

  def apply(compound: NBTTagCompound): Loc4 = {
    if (compound == null) null
    else {
      val loc = new Loc4Indirect(0, new BlockPos(0, 0, 0))
      loc.deserializeNBT(compound)
      loc
    }
  }


  def apply(): Loc4 = ORIGIN.copy()

  def apply(x: Int, y: Int, z: Int, dim:Int) = Loc4Indirect(dim, new BlockPos(x, y, z))

  def apply(loc: BlockPos, dim: Int): Loc4Indirect = Loc4Indirect(dim, loc)

  def apply(world: World, pos: BlockPos): Loc4World = Loc4World(new WrapperWorld(world), pos)

  def apply(world: IWorld, pos: BlockPos): Loc4World = Loc4World(world, pos)

  def apply(te: TileEntity): Loc4World = Loc4World(Converter.IWorldFromWorld(te.getWorld), te.getPos)

  def apply(ite: ITileEntity): Loc4World = Loc4World(ite.getIWorld, ite.getPos)

  val X_KEY   = "x"
  val Y_KEY   = "y"
  val Z_KEY   = "z"
  val DIM_KEY = "dim"
}

case class Loc4World(var iworld: IWorld, var blockPos: BlockPos) extends Loc4 {

  override def world: IWorld = iworld

  override def dim: Int = Loc4.DimensionMapper.dimensionIdForWorld(iworld)

  override def copy(): Loc4 = Loc4World(iworld, new BlockPos(blockPos))

  override def deserializeNBT(nbt: NBTTagCompound): Unit = {
    blockPos = new BlockPos(nbt.getInteger(Loc4.X_KEY),
                            nbt.getInteger(Loc4.Y_KEY),
                            nbt.getInteger(Loc4.Z_KEY))
    iworld = Loc4.DimensionMapper.worldForDimensionId(nbt.getInteger(Loc4.DIM_KEY))
  }

  override def getPos: BlockPos = blockPos

  override def getOffset(xOffset: Int, yOffset: Int, zOffset: Int): Loc4 = Loc4World(iworld, blockPos.add(xOffset, yOffset, zOffset))
}

case class Loc4Indirect(var dimension: Int, var blockPos: BlockPos) extends Loc4 {

  override def world: IWorld = Loc4.DimensionMapper.worldForDimensionId(dimension)

  override def dim: Int = dimension

  override def getPos: BlockPos = blockPos

  override def getOffset(xOffset: Int, yOffset: Int, zOffset: Int): Loc4 = Loc4Indirect(dimension, blockPos.add(xOffset, yOffset, zOffset))

  override def copy(): Loc4 = Loc4Indirect(dim, new BlockPos(blockPos))

  override def deserializeNBT(nbt: NBTTagCompound): Unit = {
    blockPos = new BlockPos(nbt.getInteger(Loc4.X_KEY),
                            nbt.getInteger(Loc4.Y_KEY),
                            nbt.getInteger(Loc4.Z_KEY))
    dimension = nbt.getInteger(Loc4.DIM_KEY)
  }
}

sealed abstract class Loc4 extends INBTSerializable[NBTTagCompound] with Comparable[Loc4] {
  def world: IWorld

  def dim: Int

  def x: Int = getPos.getX

  def y: Int = getPos.getY

  def z: Int = getPos.getZ

  def getPos: BlockPos

  def getWorld: Option[IWorld] = Option(world)

  def anchor(world: IWorld): Loc4World = Loc4World(world, getPos)

  def copy(): Loc4

  override def serializeNBT(): NBTTagCompound = {
    val ret = new NBTTagCompound
    ret.setInteger(Loc4.X_KEY, x)
    ret.setInteger(Loc4.Y_KEY, y)
    ret.setInteger(Loc4.Z_KEY, z)
    ret.setInteger(Loc4.DIM_KEY, dim)
    ret
  }

  def getITileEntity(force: Boolean = false): Option[ITileEntity] = getWorld match {
    case Some(a) => Option(if (a.isBlockLoaded(getPos) || force) a.getITileEntity(getPos) else null)
    case None => None
  }

  def getBlock(force: Boolean = false): Option[Block] = getBlockState(force).map(_.getBlock)

  def getBlockState(force: Boolean = false): Option[IBlockState] = getWorld match {
    case Some(a) => Option(if (a.isBlockLoaded(getPos) || force) a.getBlockState(getPos) else null)
    case None => None
  }

  def getChunk(force: Boolean = false): Option[IChunk] = getWorld match {
    case Some(a) => Option(if (a.isBlockLoaded(getPos) || force) a.getChunkFromBlockCoords(getPos) else null)
    case None => None
  }

  def chunkCoords: ChunkCoord = ChunkCoord(getPos)

  def chunkContains(chunk: IChunk): Boolean = {
    if (Loc4.DimensionMapper.dimensionIdForWorld(chunk.world) != dim) false
    else ChunkCoord(chunk.x, chunk.z) == chunkCoords
  }

  def isNeighbor(loc: Loc4): Boolean = {
    if (loc.dim != dim) false
    else if ((Math.abs(loc.x - x) == 1) && loc.y == y && loc.z == z) true //x
    else if (loc.x == x && (Math.abs(loc.y - y) == 1) && loc.z == z) true //y
    else if (loc.x == x && loc.y == y && (Math.abs(loc.z - z) == 1)) true //z
    else false
  }

  def getOffset(dir: EnumFacing, distance: Int = 1): Loc4 = getOffset(distance * dir.getFrontOffsetX,
                                                                      distance * dir.getFrontOffsetY,
                                                                      distance * dir.getFrontOffsetZ)


  def getOffset(xOffset: Int, yOffset: Int, zOffset: Int): Loc4

  def distSqr(other: Loc4): Double = {
    if (other.dim != dim) return Float.MaxValue
    distSqr(other.x, other.y, other.z)
  }

  def distSqr(x: Int, y: Int, z: Int): Double =
    (this.x - x) * (this.x - x) + (this.y - y) * (this.y - y) + (this.z - z) * (this.z - z)

  def dist(other: Loc4): Double = {
    if (other.dim != dim) return Float.MaxValue
    dist(other.x, other.y, other.z)
  }

  def dist(x: Int, y: Int, z: Int): Double = Math.sqrt(distSqr(x, y, z))

  def compareTo(o: Loc4): Int = {
    if (x < o.x) return -1
    if (x > o.x) return 1
    if (y < o.y) return -1
    if (y > o.y) return 1
    if (z < o.z) return -1
    if (z > o.z) return 1
    if (dim < o.dim) return -1
    if (dim > o.dim) return 1
    0
  }

  def canEqual(other: Any): Boolean = other.isInstanceOf[Loc4]

  override def equals(other: Any): Boolean = other match {
    case that: Loc4 =>
      (that canEqual this) &&
      dim == that.dim &&
      x == that.x &&
      y == that.y &&
      z == that.z
    case _ => false
  }

  override def hashCode(): Int = {
    val state = Seq(dim, x, y, z)
    state.map(_.hashCode()).foldLeft(0)((a, b) => 31 * a + b)
  }
}

/*
case class Loc4(var x: Int, var y: Int, var z: Int, var dim: Int) extends INBTSerializable[NBTTagCompound] with Comparable[Loc4] {

}
 */
