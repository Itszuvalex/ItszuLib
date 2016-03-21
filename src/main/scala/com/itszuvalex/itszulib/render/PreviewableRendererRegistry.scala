package com.itszuvalex.itszulib.render

import com.itszuvalex.itszulib.api.IPreviewableRenderer
import cpw.mods.fml.relauncher.{Side, SideOnly}

import scala.collection._

/**
  * Created by Christopher Harris (Itszuvalex) on 8/26/15.
  */
@SideOnly(Side.CLIENT)
object PreviewableRendererRegistry {
  private val renderMap = mutable.HashMap[Int, IPreviewableRenderer]()
  private var lastID    = 0

  def bindRenderer(renderer: IPreviewableRenderer): Int = {
    val id = lastID
    renderMap(id) = renderer
    lastID += 1
    id
  }

  def getRenderer(id: Int) = renderMap.get(id)

}
