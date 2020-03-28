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

import com.itszuvalex.itszulib.util.Color
import net.minecraft.block.Block
import net.minecraft.block.state.IBlockState
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.inventory.GuiContainer
import net.minecraft.client.renderer._
import net.minecraft.client.renderer.texture.{TextureAtlasSprite, TextureMap}
import net.minecraft.client.renderer.vertex.{DefaultVertexFormats, VertexFormat}
import net.minecraft.util.EnumFacing
import net.minecraft.util.EnumFacing._
import org.lwjgl.opengl.GL11

object RenderUtils {

  def drawBlock(format: VertexFormat = DefaultVertexFormats.POSITION_TEX)(func: => Unit): Unit = {
    startDrawing(format)
    try {
      func
    }
    finally finishDrawing()
  }

  def glMatrixBlock(func: => Unit): Unit = {
    GL11.glPushMatrix()
    try {
      func
    }
    finally GL11.glPopMatrix()
  }

  def translationBlock(x: Double, y: Double, z: Double)(func: => Unit): Unit = {
    GL11.glTranslated(x, y, z)
    try {
      func
    }
    finally GL11.glTranslated(-x, -y, -z)
  }

  def rotationBlock(angle: Double, xrot: Double, yrot: Double, zrot: Double)(func: => Unit) = {
    GL11.glRotated(angle, xrot, yrot, zrot)
    try {
      func
    }
    finally GL11.glRotated(-angle, xrot, yrot, zrot)
  }

  def getDefaultTextureForBlock(block: Block): TextureAtlasSprite = getTextureForBlockInState(block, block.getDefaultState)

  def getTextureForBlockInState(block: Block, state: IBlockState) = Minecraft.getMinecraft.getBlockRendererDispatcher.getBlockModelShapes.getTexture(state)

  def setNormal(x: Double, y: Double, z: Double) = {
    GL11.glNormal3d(x, y, z)
  }

  def renderCube(x: Float, y: Float, z: Float, startx: Float, starty: Float, startz: Float, endx: Float, endy: Float, endz: Float, texture: TextureAtlasSprite) {
    renderCube(x, y, z, startx, starty, startz, endx, endy, endz, texture, texture.getMinU, texture.getMaxU, texture.getMinV, texture.getMaxV)
  }

  def renderCube(x: Float, y: Float, z: Float, startx: Float, starty: Float, startz: Float, endx: Float, endy: Float, endz: Float, texture: TextureAtlasSprite, minU: Float, maxU: Float, minV: Float, maxV: Float) {
    drawTopFace(x, y, z, startx, endx, startz, endz, endy, texture, minU, maxU, minV, maxV)
    drawBottomFace(x, y, z, startx, endx, startz, endz, starty, texture, minU, maxU, minV, maxV)
    drawNorthFace(x, y, z, startx, endx, starty, endy, startz, texture, minU, maxU, minV, maxV)
    drawEastFace(x, y, z, starty, endy, startz, endz, endx, texture, minU, maxU, minV, maxV)
    drawSouthFace(x, y, z, startx, endx, starty, endy, endz, texture, minU, maxU, minV, maxV)
    drawWestFace(x, y, z, starty, endy, startz, endz, startx, texture, minU, maxU, minV, maxV)
  }

  def drawTopFace(x: Float, y: Float, z: Float, xmin: Float, xmax: Float, zmin: Float, zmax: Float, yoffset: Float, texture: TextureAtlasSprite, minU: Float, maxU: Float, minV: Float, maxV: Float) {
    val nx = EnumFacing.UP.getFrontOffsetX.toFloat
    val ny = EnumFacing.UP.getFrontOffsetY.toFloat
    val nz = EnumFacing.UP.getFrontOffsetZ.toFloat
    translationBlock(x, y, z) {
      drawBlock(DefaultVertexFormats.POSITION_TEX_NORMAL) {
        addVertexUVNormal(xmax, yoffset, zmax, maxU, maxV, nx, ny, nz)
        addVertexUVNormal(xmax, yoffset, zmin, maxU, minV, nx, ny, nz)
        addVertexUVNormal(xmin, yoffset, zmin, minU, minV, nx, ny, nz)
        addVertexUVNormal(xmin, yoffset, zmax, minU, maxV, nx, ny, nz)
      }
    }
  }

  def drawBottomFace(x: Float, y: Float, z: Float, xmin: Float, xmax: Float, zmin: Float, zmax: Float, yoffset: Float, texture: TextureAtlasSprite, minU: Float, maxU: Float, minV: Float, maxV: Float) {
    val nx = EnumFacing.DOWN.getFrontOffsetX.toFloat
    val ny = EnumFacing.DOWN.getFrontOffsetY.toFloat
    val nz = EnumFacing.DOWN.getFrontOffsetZ.toFloat
    translationBlock(x, y, z) {
      drawBlock(DefaultVertexFormats.POSITION_TEX_NORMAL) {
        addVertexUVNormal(xmin, yoffset, zmax, maxU, maxV, nx, ny, nz)
        addVertexUVNormal(xmin, yoffset, zmin, maxU, minV, nx, ny, nz)
        addVertexUVNormal(xmax, yoffset, zmin, minU, minV, nx, ny, nz)
        addVertexUVNormal(xmax, yoffset, zmax, minU, maxV, nx, ny, nz)
      }
    }
  }

  def drawNorthFace(x: Float, y: Float, z: Float, xmin: Float, xmax: Float, ymin: Float, ymax: Float, zoffset: Float, texture: TextureAtlasSprite, minU: Float, maxU: Float, minV: Float, maxV: Float) {
    val nx = EnumFacing.NORTH.getFrontOffsetX.toFloat
    val ny = EnumFacing.NORTH.getFrontOffsetY.toFloat
    val nz = EnumFacing.NORTH.getFrontOffsetZ.toFloat
    translationBlock(x, y, z) {
      drawBlock(DefaultVertexFormats.POSITION_TEX_NORMAL) {
        addVertexUVNormal(xmin, ymin, zoffset, maxU, maxV, nx, ny, nz)
        addVertexUVNormal(xmin, ymax, zoffset, maxU, minV, nx, ny, nz)
        addVertexUVNormal(xmax, ymax, zoffset, minU, minV, nx, ny, nz)
        addVertexUVNormal(xmax, ymin, zoffset, minU, maxV, nx, ny, nz)
      }
    }
  }

  def drawEastFace(x: Float, y: Float, z: Float, ymin: Float, ymax: Float, zmin: Float, zmax: Float, xoffset: Float, texture: TextureAtlasSprite, minU: Float, maxU: Float, minV: Float, maxV: Float) {
    val nx = EnumFacing.EAST.getFrontOffsetX.toFloat
    val ny = EnumFacing.EAST.getFrontOffsetY.toFloat
    val nz = EnumFacing.EAST.getFrontOffsetZ.toFloat
    translationBlock(x, y, z) {
      drawBlock(DefaultVertexFormats.POSITION_TEX_NORMAL) {
        addVertexUVNormal(xoffset, ymin, zmin, maxU, maxV, nx, ny, nz)
        addVertexUVNormal(xoffset, ymax, zmin, maxU, minV, nx, ny, nz)
        addVertexUVNormal(xoffset, ymax, zmax, minU, minV, nx, ny, nz)
        addVertexUVNormal(xoffset, ymin, zmax, minU, maxV, nx, ny, nz)
      }
    }
  }

  def drawSouthFace(x: Float, y: Float, z: Float, xmin: Float, xmax: Float, ymin: Float, ymax: Float, zoffset: Float, texture: TextureAtlasSprite, minU: Float, maxU: Float, minV: Float, maxV: Float) {
    val nx = EnumFacing.SOUTH.getFrontOffsetX.toFloat
    val ny = EnumFacing.SOUTH.getFrontOffsetY.toFloat
    val nz = EnumFacing.SOUTH.getFrontOffsetZ.toFloat
    translationBlock(x, y, z) {
      drawBlock(DefaultVertexFormats.POSITION_TEX_NORMAL) {
        addVertexUVNormal(xmax, ymin, zoffset, maxU, maxV, nx, ny, nz)
        addVertexUVNormal(xmax, ymax, zoffset, maxU, minV, nx, ny, nz)
        addVertexUVNormal(xmin, ymax, zoffset, minU, minV, nx, ny, nz)
        addVertexUVNormal(xmin, ymin, zoffset, minU, maxV, nx, ny, nz)
      }
    }
  }

  def drawWestFace(x: Float, y: Float, z: Float, ymin: Float, ymax: Float, zmin: Float, zmax: Float, xoffset: Float, texture: TextureAtlasSprite, minU: Float, maxU: Float, minV: Float, maxV: Float) {
    val nx = EnumFacing.WEST.getFrontOffsetX.toFloat
    val ny = EnumFacing.WEST.getFrontOffsetY.toFloat
    val nz = EnumFacing.WEST.getFrontOffsetZ.toFloat
    translationBlock(x, y, z) {
      drawBlock(DefaultVertexFormats.POSITION_TEX_NORMAL) {
        addVertexUVNormal(xoffset, ymin, zmax, maxU, maxV, nx, ny, nz)
        addVertexUVNormal(xoffset, ymax, zmax, maxU, minV, nx, ny, nz)
        addVertexUVNormal(xoffset, ymax, zmin, minU, minV, nx, ny, nz)
        addVertexUVNormal(xoffset, ymin, zmin, minU, maxV, nx, ny, nz)
      }
    }
  }

  def renderDoubleSidedCube(x: Float, y: Float, z: Float, startx: Float, starty: Float, startz: Float, endx: Float, endy: Float, endz: Float, texture: TextureAtlasSprite) {
    drawTopFace(x, y, z, startx, endx, startz, endz, endy, texture, texture.getMinU, texture.getMaxU, texture.getMinV, texture.getMaxV)
    drawBottomFace(x, y, z, startx, endx, startz, endz, endy, texture, texture.getMinU, texture.getMaxU, texture.getMinV, texture.getMaxV)
    drawBottomFace(x, y, z, startx, endx, startz, endz, starty, texture, texture.getMinU, texture.getMaxU, texture.getMinV, texture.getMaxV)
    drawTopFace(x, y, z, startx, endx, startz, endz, starty, texture, texture.getMinU, texture.getMaxU, texture.getMinV, texture.getMaxV)
    drawNorthFace(x, y, z, startx, endx, starty, endy, startz, texture, texture.getMinU, texture.getMaxU, texture.getMinV, texture.getMaxV)
    drawSouthFace(x, y, z, startx, endx, starty, endy, startz, texture, texture.getMinU, texture.getMaxU, texture.getMinV, texture.getMaxV)
    drawEastFace(x, y, z, starty, endy, startz, endz, endx, texture, texture.getMinU, texture.getMaxU, texture.getMinV, texture.getMaxV)
    drawWestFace(x, y, z, starty, endy, startz, endz, endx, texture, texture.getMinU, texture.getMaxU, texture.getMinV, texture.getMaxV)
    drawSouthFace(x, y, z, startx, endx, starty, endy, endz, texture, texture.getMinU, texture.getMaxU, texture.getMinV, texture.getMaxV)
    drawNorthFace(x, y, z, startx, endx, starty, endy, endz, texture, texture.getMinU, texture.getMaxU, texture.getMinV, texture.getMaxV)
    drawWestFace(x, y, z, starty, endy, startz, endz, startx, texture, texture.getMinU, texture.getMaxU, texture.getMinV, texture.getMaxV)
    drawEastFace(x, y, z, starty, endy, startz, endz, startx, texture, texture.getMinU, texture.getMaxU, texture.getMinV, texture.getMaxV)
  }

  def makeTopFace(xmin: Float, xmax: Float, zmin: Float, zmax: Float, yoffset: Float, texture: TextureAtlasSprite, minU: Float, maxU: Float, minV: Float, maxV: Float): RenderQuad = {
    val a = new Point3D
    val b = new Point3D
    val c = new Point3D
    val d = new Point3D
    a.y = {b.y = {c.y = {d.y = yoffset; d.y}; c.y}; b.y}
    a.x = {b.x = xmin; b.x}
    c.x = {d.x = xmax; d.x}
    a.z = {d.z = zmin; d.z}
    b.z = {c.z = zmax; c.z}
    new RenderQuad(a, b, c, d, texture, minU, maxU, minV, maxV)
  }

  def makeBottomFace(xmin: Float, xmax: Float, zmin: Float, zmax: Float, yoffset: Float, texture: TextureAtlasSprite, minU: Float, maxU: Float, minV: Float, maxV: Float): RenderQuad = {
    val a = new Point3D
    val b = new Point3D
    val c = new Point3D
    val d = new Point3D
    a.y = {b.y = {c.y = {d.y = yoffset; d.y}; c.y}; b.y}
    a.x = {d.x = xmin; d.x}
    b.x = {c.x = xmax; c.x}
    a.z = {b.z = zmin; b.z}
    c.z = {d.z = zmax; d.z}
    new RenderQuad(a, b, c, d, texture, minU, maxU, minV, maxV)
  }

  def makeNorthFace(xmin: Float, xmax: Float, ymin: Float, ymax: Float, zoffset: Float, texture: TextureAtlasSprite, minU: Float, maxU: Float, minV: Float, maxV: Float): RenderQuad = new RenderQuad(new Point3D(xmax, ymax, zoffset), new Point3D(xmax, ymin, zoffset), new Point3D(xmin, ymin, zoffset), new Point3D(xmin, ymax, zoffset), texture, minU, maxU, minV, maxV)

  def makeEastFace(ymin: Float, ymax: Float, zmin: Float, zmax: Float, xoffset: Float, texture: TextureAtlasSprite, minU: Float, maxU: Float, minV: Float, maxV: Float): RenderQuad = new RenderQuad(new Point3D(xoffset, ymin, zmin), new Point3D(xoffset, ymax, zmin), new Point3D(xoffset, ymax, zmax), new Point3D(xoffset, ymin, zmax), texture, minU, maxU, minV, maxV)

  def makeWestFace(ymin: Float, ymax: Float, zmin: Float, zmax: Float, xoffset: Float, texture: TextureAtlasSprite, minU: Float, maxU: Float, minV: Float, maxV: Float): RenderQuad = new RenderQuad(new Point3D(xoffset, ymin, zmin), new Point3D(xoffset, ymin, zmax), new Point3D(xoffset, ymax, zmax), new Point3D(xoffset, ymax, zmin), texture, minU, maxU, minV, maxV)

  def makeSouthFace(xmin: Float, xmax: Float, ymin: Float, ymax: Float, zoffset: Float, texture: TextureAtlasSprite, minU: Float, maxU: Float, minV: Float, maxV: Float): RenderQuad = new RenderQuad(new Point3D(xmin, ymax, zoffset), new Point3D(xmin, ymin, zoffset), new Point3D(xmax, ymin, zoffset), new Point3D(xmax, ymax, zoffset), texture, minU, maxU, minV, maxV)

  def drawArbitraryFace(x: Float, y: Float, z: Float, xmin: Float, xmax: Float, ymin: Float, ymax: Float, zmin: Float, zmax: Float, normal: EnumFacing, texture: TextureAtlasSprite, minU: Float, maxU: Float, minV: Float, maxV: Float) {
    normal match {
      case UP => drawTopFace(x, y, z, xmin, xmax, zmin, zmax, ymax, texture, minU, maxU, minV, maxV)
      case DOWN => drawBottomFace(x, y, z, xmin, xmax, zmin, zmax, ymin, texture, minU, maxU, minV, maxV)
      case NORTH => drawNorthFace(x, y, z, xmin, xmax, ymin, ymax, zmin, texture, minU, maxU, minV, maxV)
      case EAST => drawEastFace(x, y, z, ymin, ymax, zmin, zmax, xmax, texture, minU, maxU, minV, maxV)
      case SOUTH => drawSouthFace(x, y, z, xmin, xmax, ymin, ymax, zmax, texture, minU, maxU, minV, maxV)
      case WEST => drawWestFace(x, y, z, ymin, ymax, zmin, zmax, xmin, texture, minU, maxU, minV, maxV)
      case _ =>
    }
  }

  def drawFaceByPoints(x: Float, y: Float, z: Float, A: Point3D, B: Point3D, C: Point3D, D: Point3D, texture: TextureAtlasSprite, minU: Float, maxU: Float, minV: Float, maxV: Float) {
    drawBlock() {
      addVertexUV(A.x + x, A.y + y, A.z + z, minU, maxV)
      addVertexUV(B.x + x, B.y + y, B.z + z, minU, minV)
      addVertexUV(C.x + x, C.y + y, C.z + z, maxU, minV)
      addVertexUV(D.x + x, D.y + y, D.z + z, maxU, maxV)
    }
  }

  def drawBillboard(x: Double, y: Double, z: Double, rot: Float, scale: Double, uMin: Float = 0, uMax: Float = 1, vMin: Float = 0, vMax: Float = 1): Unit = {
    drawBillboardPerpendicular(x, y, z, rot, scale, uMin, uMax, vMin, vMax)
  }

  def drawBillboardPerpendicular(x: Double, y: Double, z: Double, rot: Float, scale: Double, uMin: Float = 0, uMax: Float = 1, vMin: Float = 0, vMax: Float = 1): Unit = {
    val xRot = ActiveRenderInfo.getRotationX
    val zRot = ActiveRenderInfo.getRotationZ
    val rotYZ = ActiveRenderInfo.getRotationYZ
    val rotXY = ActiveRenderInfo.getRotationXY
    val rotXZ = ActiveRenderInfo.getRotationXZ

    drawBlock() {
      addVertexUV(x - xRot * scale - rotYZ * scale, y - rotXZ * scale, z - zRot * scale - rotXY * scale, uMax, vMax)
      addVertexUV(x - xRot * scale + rotYZ * scale, y + rotXZ * scale, z - zRot * scale + rotXY * scale, uMax, vMin)
      addVertexUV(x + xRot * scale + rotYZ * scale, y + rotXZ * scale, z + zRot * scale + rotXY * scale, uMin, vMin)
      addVertexUV(x + xRot * scale - rotYZ * scale, y - rotXZ * scale, z + zRot * scale - rotXY * scale, uMin, vMax)
    }
  }

  def drawBillboardFacingCamera(x: Double, y: Double, z: Double, dx: Double, dy: Double, dz: Double, rot: Float, scale: Double, uMin: Float = 0, uMax: Float = 1, vMin: Float = 0, vMax: Float = 1): Unit = {
    drawBillboardFacing(x, y, z, 0, 0, 0, rot, scale, uMin, uMax, vMin, vMax)
  }

  def drawBillboardFacing(x: Double, y: Double, z: Double, dx: Double, dy: Double, dz: Double, rot: Float, scale: Double, uMin: Float = 0, uMax: Float = 1, vMin: Float = 0, vMax: Float = 1): Unit = {
    val billPos = Vector3(x, y, z)

    val cameraDiff = Vector3(dx, dy, dz) - billPos
    val up = Vector3(0, 1, 0)
    val right = cameraDiff.cross(up).normalize()
    val facingUp = cameraDiff.cross(right).normalize()
    drawBillboardUpRightVector(billPos, facingUp, right, uMin, uMax, vMin, vMax)
  }

  def drawBillboardUpRightVector(billPos: Vector3, upVector: Vector3, rightVector: Vector3, uMin: Float = 0, uMax: Float = 1, vMin: Float = 0, vMax: Float = 1): Unit = {
    val pos1 = billPos + rightVector - upVector
    val pos2 = billPos + rightVector + upVector
    val pos3 = billPos - rightVector + upVector
    val pos4 = billPos - rightVector - upVector

    drawBlock() {
      addVertexUV(pos1.x, pos1.y, pos1.z, uMin, vMin)
      addVertexUV(pos2.x, pos2.y, pos2.z, uMin, vMax)
      addVertexUV(pos3.x, pos3.y, pos3.z, uMax, vMax)
      addVertexUV(pos4.x, pos4.y, pos4.z, uMax, vMin)
    }
  }

  def drawBillboardFacingUp(x: Double, y: Double, z: Double, dx: Double, dy: Double, dz: Double, rot: Float, scale: Double, uMin: Float = 0, uMax: Float = 1, vMin: Float = 0, vMax: Float = 1): Unit = {
    val billPos = Vector3(x, y, z)

    val cameraDiff = Vector3(dx, dy, dz) - billPos
    val up = Vector3(0, 1, 0)
    val right = cameraDiff.cross(up).normalize()

    drawBillboardUpRightVector(billPos, up, right, uMin, uMax, vMin, vMax)
  }

  def renderLiquidInGUI(container: GuiContainer, zheight: Float, icon: TextureAtlasSprite, x: Int, y: Int, width: Int, height: Int) {
    bindBlockTextures()
    renderLiquidInGUI_height(container, zheight, icon, x, y, width, height)
  }

  private def renderLiquidInGUI_height(container: GuiContainer, zheight: Float, icon: TextureAtlasSprite, x: Int, y: Int, width: Int, height: Int) {
    var i = 0
    var remaining = height
    if ((height - width) > 0) {
      {
        i = 0
        while (width < remaining) {
          {
            drawTexturedModalSquareFromIcon(zheight, x, y + i, width, icon)
          }
          i += width
          remaining -= width
        }
      }
    }
    drawTexturedModalRectFromIcon(zheight, x, y + i, width, remaining, icon.getMinU, icon.getMaxU, icon.getMinV, icon.getInterpolatedV((remaining * 16f) / width))
  }

  def drawLine(x1: Int, x2: Int, y1: Int, y2: Int, width: Int, color: Int) {
    val difX = (x2 - x1).toFloat
    val difY = (y2 - y1).toFloat
    val length = Math.sqrt(Math.pow(difX, 2) + Math.pow(difY, 2)).toFloat
    val xS = (width * difY / length) / 2f
    val yS = (width * difX / length) / 2f
    val alpha = (color >> 24 & 255).toFloat / 255.0F
    val red = (color >> 16 & 255).toFloat / 255.0F
    val green = (color >> 8 & 255).toFloat / 255.0F
    val blue = (color & 255).toFloat / 255.0F
    GL11.glEnable(GL11.GL_BLEND)
    GL11.glDisable(GL11.GL_TEXTURE_2D)
    GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)
    GL11.glColor4f(red, green, blue, alpha)

    //Taken from net.minecraft.client.Gui
    drawBlock() {
      addVertex(x2.toDouble - xS, y2.toDouble + yS, 0.0D).endVertex()
      addVertex(x2.toDouble + xS, y2.toDouble - yS, 0.0D).endVertex()
      addVertex(x1.toDouble + xS, y1.toDouble - yS, 0.0D).endVertex()
      addVertex(x1.toDouble - xS, y1.toDouble + yS, 0.0D).endVertex()
    }
    GL11.glEnable(GL11.GL_TEXTURE_2D)
    GL11.glDisable(GL11.GL_BLEND)
  }

  def startDrawing(format: VertexFormat) {
    Tessellator.getInstance().getBuffer.begin(7, format)
  }

  def addVertex(x: Double, y: Double, z: Double): BufferBuilder = {
    Tessellator.getInstance().getBuffer.pos(x, y, z)
  }

  def addVertex(point: Point3D): BufferBuilder = addVertex(point.x, point.y, point.z)

  def addVertexUV(x: Double, y: Double, z: Double, u: Double, v: Double): Unit = {
    addVertex(x, y, z).tex(u, v).endVertex()
  }

  def addVertexUV(point: Point3D, u: Double, v: Double): Unit = addVertexUV(point.x, point.y, point.z, u, v)

  def addVertexUVColor(x: Double, y: Double, z: Double, u: Double, v: Double, color: Color): Unit = {
    addVertex(x, y, z).tex(u, v).color(color.red, color.green, color.blue, color.alpha).endVertex()
  }

  def addVertexUVColor(point: Point3D, u: Double, v: Double, color: Color): Unit = addVertexUVColor(point.x, point.y, point.z, u, v, color)

  def addVertexUVNormal(x: Double, y: Double, z: Double, u: Double, v: Double, nx: Float, ny: Float, nz: Float): Unit = {
    addVertex(x, y, z).tex(u, v).normal(nx, ny, nz).endVertex()
  }

  def addVertexUVNormal(point: Point3D, u: Double, v: Double, normal: Vector3): Unit = addVertexUVNormal(point.x, point.y, point.z, u, v, normal.x.toFloat, normal.y.toFloat, normal.z.toFloat)

  def addVertexUVColorNormal(x: Double, y: Double, z: Double, u: Double, v: Double, color: Color, nx: Float, ny: Float, nz: Float): Unit = {
    addVertex(x, y, z).tex(u, v).color(color.red, color.green, color.blue, color.alpha).normal(nx, ny, nz).endVertex()
  }

  def addVertexUVColorNormal(point: Point3D, u: Double, v: Double, color: Color, normal: Vector3): Unit = addVertexUVColorNormal(point.x, point.y, point.z, u, v, color, normal.x.toFloat, normal.y.toFloat, normal.z.toFloat)

  def finishDrawing(): Unit = {
    Tessellator.getInstance().draw()
  }

  private def renderLiquidInGUI_width(container: GuiContainer, zheight: Float, icon: TextureAtlasSprite, x: Int, y: Int, width: Int, height: Int) {
    var i = 0
    var remaining = width
    if ((width - height) > 0) {
      {
        i = 0
        while (height < remaining) {
          {
            drawTexturedModalSquareFromIcon(zheight, x + i, y, height, icon)
          }
          i += height
          remaining -= height
        }
      }
    }
    drawTexturedModalRectFromIcon(zheight, x + i, y, remaining, height, icon.getMinU, icon.getInterpolatedV((remaining * 16f) / height), icon.getMinV, icon.getMaxV)
  }

  private def drawTexturedModalSquareFromIcon(zheight: Float, x: Int, y: Int, size: Int, icon: TextureAtlasSprite) {
    drawTexturedModalRectFromIcon(zheight, x, y, size, size, icon.getMinU, icon.getMaxU, icon.getMinV, icon.getMaxV)
  }

  private def drawTexturedModalRectFromIcon(zheight: Float, x: Int, y: Int, width: Int, height: Int, minU: Float, maxU: Float, minV: Float, maxV: Float) {
    //Taken from net.minecraft.client.Gui
    drawBlock() {
      addVertexUV(x.toDouble, (y + height).toDouble, zheight.toDouble, minU.toDouble, maxV.toDouble)
      addVertexUV((x + width).toDouble, (y + height).toDouble, zheight.toDouble, maxU.toDouble, maxV.toDouble)
      addVertexUV((x + width).toDouble, y.toDouble, zheight.toDouble, maxU.toDouble, minV.toDouble)
      addVertexUV(x.toDouble, y.toDouble, zheight.toDouble, minU.toDouble, minV.toDouble)
    }
  }

  def bindBlockTextures(): Unit = {
    Minecraft.getMinecraft.getTextureManager.bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE)
    Minecraft.getMinecraft.getTextureManager.getTexture(TextureMap.LOCATION_BLOCKS_TEXTURE).setBlurMipmap(false, false)
  }
}

