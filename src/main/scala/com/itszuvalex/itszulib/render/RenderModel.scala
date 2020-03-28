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

import net.minecraft.client.renderer.Tessellator
import net.minecraft.util.EnumFacing
import net.minecraft.util.EnumFacing._

import scala.collection.mutable.ArrayBuffer

class RenderModel(var location: Point3D, var center: Point3D) {
  val faces = new ArrayBuffer[RenderQuad]

  def this(location: Point3D) = this(location, Point3D(0, 0, 0))

  def this() = this(Point3D(0, 0, 0))

  def removeQuad(quad: RenderQuad): Unit = faces -= quad

  def rotatedOnXAxis(rot: Double): RenderModel = rotatedOnXAxis(rot, center.y, center.z)

  def rotatedOnXAxis(rot: Double, yrotoffset: Float, zrotoffset: Float): RenderModel = copy.rotateOnXAxis(rot, yrotoffset, zrotoffset)

  def rotatedOnYAxis(rot: Double): RenderModel = rotatedOnYAxis(rot, center.x, center.z)

  def rotatedOnYAxis(rot: Double, xrotoffset: Float, zrotoffset: Float): RenderModel = copy.rotateOnYAxis(rot, xrotoffset, zrotoffset)

  def rotatedOnZAxis(rot: Double): RenderModel = rotatedOnZAxis(rot, center.x, center.y)

  def rotatedOnZAxis(rot: Double, xrotoffset: Float, yrotoffset: Float): RenderModel = copy.rotateOnZAxis(rot, xrotoffset, yrotoffset)

  def rotate(x: Double, y: Double, z: Double): RenderModel = rotateOnXAxis(x).rotateOnYAxis(y).rotateOnZAxis(z)

  def rotated(x: Double, y: Double, z: Double): RenderModel = copy.rotateOnXAxis(x).rotateOnYAxis(y).rotateOnZAxis(z)

  def rotateOnZAxis(rot: Double, xrotoffset: Float, yrotoffset: Float): RenderModel = {
    faces.foreach(_.rotateOnZAxis(rot, xrotoffset, yrotoffset))
    this
  }

  def rotateOnYAxis(rot: Double, xrotoffset: Float, zrotoffset: Float): RenderModel = {
    faces.foreach(_.rotateOnYAxis(rot, xrotoffset, zrotoffset))
    this
  }

  def rotateOnZAxis(rot: Double): RenderModel = rotateOnZAxis(rot, center.x, center.y)

  def rotateOnXAxis(rot: Double, yrotoffset: Float, zrotoffset: Float): RenderModel = {
    faces.foreach(_.rotateOnXAxis(rot, yrotoffset, zrotoffset))
    this
  }

  def rotateOnYAxis(rot: Double): RenderModel = rotateOnYAxis(rot, center.x, center.z)

  def draw() {
    val tes = Tessellator.getInstance()
    //    tes.addTranslation(location.x, location.y, location.z)
    faces.foreach(_.draw())
    //    tes.addTranslation(-location.x, -location.y, -location.z)
  }

  def rotateOnXAxis(rot: Double): RenderModel = rotateOnXAxis(rot, center.y, center.z)

  def rotatedToDirection(dir: EnumFacing): RenderModel = dir match {
    case SOUTH => rotatedOnXAxis(Math.PI)
    case EAST => rotatedOnYAxis(-Math.PI / 2d)
    case WEST => rotatedOnYAxis(Math.PI / 2d)
    case UP => rotatedOnXAxis(Math.PI / 2d)
    case DOWN => rotatedOnXAxis(-Math.PI / 2d)
    case _ => copy
  }

  def copy: RenderModel = {
    val ret = new RenderModel(location.copy, center.copy)
    faces.foreach(quad => ret.addQuad(quad.copy))
    ret
  }

  def addQuad(quad: RenderQuad): Unit = faces += quad
}
