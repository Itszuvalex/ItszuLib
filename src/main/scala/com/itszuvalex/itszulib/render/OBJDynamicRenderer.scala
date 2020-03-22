package com.itszuvalex.itszulib.render

import com.itszuvalex.itszulib.render.RenderUtils._
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.vertex.DefaultVertexFormats
import net.minecraft.util.ResourceLocation
import net.minecraftforge.client.model.ModelLoaderRegistry
import net.minecraftforge.client.model.obj.OBJModel
import net.minecraftforge.fml.relauncher.{Side, SideOnly}

import scala.collection.JavaConversions._

/**
  * Created by Chris on 8/7/2016.
  */
@SideOnly(Side.CLIENT)
object OBJDynamicRenderer {

  def LoadObj(loc: ResourceLocation): OBJModel = {
    val model = ModelLoaderRegistry.getModelOrMissing(loc)
    if (!model.isInstanceOf[OBJModel]) null
    else model.asInstanceOf[OBJModel]
  }

  implicit class ObjRender(model: OBJModel) {
    def render(bindTextures: Boolean = false): Unit = {
      if (model == null) return

      renderGroups(model.getMatLib.getGroups.map(_._1).toSet, bindTextures)
    }

    def renderGroups(groups: Set[String], bindTextures: Boolean = false): Unit = {
      if (model == null) return

      val matLib  = model.getMatLib
      val rgroups = matLib.getGroups.filter(g => groups.contains(g._1)).values.view
      drawBlock(DefaultVertexFormats.POSITION_TEX_NORMAL) {
        rgroups.foreach { group =>
          val faces = group.getFaces
          faces.foreach { face =>
            if (bindTextures) {
              val mat    = matLib.getMaterial(face.getMaterialName)
              val texLoc = mat.getTexture.getTextureLocation
              Minecraft.getMinecraft.getTextureManager.bindTexture(texLoc)
            }
            val normal = face.getNormal
            val verts  = face.getVertices
            verts.foreach { vert =>
              val vertU = Option(vert.getTextureCoordinate).map(_.u).getOrElse(0f)
              val vertV = Option(vert.getTextureCoordinate).map(_.v).getOrElse(0f)
              addVertexUVNormal(vert.getPos3.getX, vert.getPos3.getY, vert.getPos3.getZ,
                                vertU, 1d - vertV,
                                normal.x, normal.y, normal.z)
            }
          }
        }
      }
    }
  }

}
