package com.itszuvalex.itszulib.testing

import com.itszuvalex.itszulib.api.IPreviewable
import net.minecraft.item.Item

/**
  * Created by Christopher Harris (Itszuvalex) on 8/26/15.
  */
class ItemPreviewable extends Item with IPreviewable {

  /**
    *
    * @return The ID of IPreviewableRenderer.  This is separate from Forge RenderIDs.
    */
  override def renderID: Int = PreviewableIDs.testID
}
