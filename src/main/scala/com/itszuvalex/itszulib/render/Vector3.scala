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
package com.itszuvalex.itszulib.render

import net.minecraft.nbt.NBTTagCompound
import net.minecraftforge.common.util.INBTSerializable

/**
  * Created by Christopher Harris (Itszuvalex) on 5/16/14.
  */
case class Vector3(var x: Double, var y: Double, var z: Double) extends INBTSerializable[NBTTagCompound] {

  def this(a: Point3D, b: Point3D) =
    this(a.x - b.x, a.y - b.y, a.z - b.z)

  def this() =
    this(0, 0, 0)

  def +(other: Vector3): Vector3 = Vector3(x + other.x, y + other.y, z + other.z)

  def +=(other: Vector3): Unit = {
    x += other.x
    y += other.y
    z += other.z
  }

  def -(other: Vector3): Vector3 = Vector3(x - other.x, y - other.y, z - other.z)

  def -=(other: Vector3): Unit = {
    x -= other.x
    y -= other.y
    z -= other.z
  }

  def /(other: Float): Vector3 = this * (1 / other)

  def *(other: Float): Vector3 = Vector3(x * other, y * other, z * other)

  def /=(other: Float): Unit = this *= (1 / other)

  def *=(other: Float): Unit = {
    x *= other
    y *= other
    z *= other
  }

  def inversed: Vector3 = copy().inverse()

  def inverse(): Vector3 = {
    x = -x
    y = -y
    z = -z
    this
  }

  def normalized: Vector3 = copy().normalize()

  def copy(): Vector3 = Vector3(x, y, z)

  def normalize(): Vector3 = {
    val mag = magnitude
    x /= mag
    y /= mag
    z /= mag
    this
  }

  def magnitude: Double = Math.sqrt(magnitudeSquared)

  def magnitudeSquared: Double = x * x + y * y + z * z

  def cross(vector: Vector3): Vector3 = Vector3(y * vector.z - z * vector.y, z * vector.x - x * vector.z, x * vector.y - y * vector.x)

  def dot(vector: Vector3): Double = x * vector.x + y * vector.y + z * vector.z

  override def serializeNBT(): NBTTagCompound = {
    val compound = new NBTTagCompound
    compound.setDouble("x", x)
    compound.setDouble("y", y)
    compound.setDouble("z", z)
    compound
  }

  override def deserializeNBT(compound: NBTTagCompound): Unit = {
    x = compound.getDouble("x")
    y = compound.getDouble("y")
    z = compound.getDouble("z")
  }
}
