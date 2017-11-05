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
import com.itszuvalex.itszulib.api.Overridable
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.EnumFacing
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import net.minecraft.world.chunk.Chunk
import net.minecraftforge.common.util.INBTSerializable

/**
  * Created by Christopher Harris (Itszuvalex) on 5/9/14.
  */
object Loc4 {
  val worldIntMapper = new Overridable((w: World) => w.provider.getDimension)
  val intWorldMapper = new Overridable[(Int) => _ <: World](ItszuLib.proxy.getWorld _)

  val ORIGIN = Loc4(0, 0, 0, 0)

  def apply(compound: NBTTagCompound): Loc4 = {
    if (compound == null) null
    else {
      val loc = new Loc4(0, 0, 0, 0)
      loc.deserializeNBT(compound)
      loc
    }
  }

  def setWorldIntMapper(func: (World) => Int) = worldIntMapper.overrideDefault(func)

  def restoreDefaultWorldIntMapper() = worldIntMapper.revert()

  def mapWorld(world: World): Int = worldIntMapper.apply(world)

  def setIntWorldMapper(func: (Int) => World) = intWorldMapper.overrideDefault(func)

  def restoreDefaultIntWorldMapper() = intWorldMapper.revert()

  def mapInt(int: Int): World = intWorldMapper.apply(int)
}

case class Loc4(var x: Int, var y: Int, var z: Int, var dim: Int) extends INBTSerializable[NBTTagCompound] with Comparable[Loc4] {

  def this() = this(0, 0, 0, 0)

  def this(loc: BlockPos, dim: Int) = this(loc.getX, loc.getY, loc.getZ, dim)

  def this(world: World, pos: BlockPos) = this(pos, Loc4.mapWorld(world))

  def this(te: TileEntity) = this(te.getWorld, te.getPos)

  override def serializeNBT(): NBTTagCompound = {
    val compound = new NBTTagCompound
    compound.setInteger("x", x)
    compound.setInteger("y", y)
    compound.setInteger("z", z)
    compound.setInteger("dim", dim)
    compound
  }

  override def deserializeNBT(compound: NBTTagCompound): Unit = {
    x = compound.getInteger("x")
    y = compound.getInteger("y")
    z = compound.getInteger("z")
    dim = compound.getInteger("dim")
  }

  def getTileEntity(force: Boolean = false) = getWorld match {
    case Some(a) => Option(if (a.isBlockLoaded(getPos) || force) a.getTileEntity(getPos) else null)
    case None => None
  }

  def getBlock(force: Boolean = false) = getBlockState(force).map(_.getBlock)

  def getBlockState(force: Boolean = false) = getWorld match {
    case Some(a) => Option(if (a.isBlockLoaded(getPos) || force) a.getBlockState(getPos) else null)
    case None => None
  }

  def getPos = new BlockPos(x, y, z)

  def getWorld = Option(Loc4.mapInt(dim))

  def getChunk(force: Boolean = false) = getWorld match {
    case Some(a) => Option(if (a.isBlockLoaded(getPos) || force) a.getChunkFromBlockCoords(getPos) else null)
    case None => None
  }

  def chunkCoords = (x >> 4, z >> 4)

  def chunkContains(chunk: Chunk): Boolean = {
    if (Loc4.mapWorld(chunk.getWorld) != dim) false
    else if (x <= chunk.x * 16) false
    else if (x > chunk.x * 16 + 16) false
    else if (z <= chunk.z * 16) false
    else if (z > chunk.z * 16 + 16) false
    else true
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


  def getOffset(xOffset: Int, yOffset: Int, zOffset: Int): Loc4 = new Loc4(x + xOffset, y + yOffset, z + zOffset, dim)

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

  def dist(x: Int, y: Int, z: Int) = Math.sqrt(distSqr(x, y, z))

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
}
