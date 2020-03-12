package com.itszuvalex.itszulib.testing

import com.itszuvalex.itszulib.api.ItszuLibCapabilities
import com.itszuvalex.itszulib.api.client.IPreviewable
import net.minecraft.item.{Item, ItemStack}
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.EnumFacing
import net.minecraftforge.common.capabilities.{Capability, ICapabilityProvider}

/**
  * Created by Christopher Harris (Itszuvalex) on 8/26/15.
  */
class ItemPreviewable extends Item {
  override def initCapabilities(stack: ItemStack, nbt: NBTTagCompound): ICapabilityProvider = {
    new ICapabilityProvider {
      override def getCapability[T](capability: Capability[T], facing: EnumFacing): T = {
        if (capability == ItszuLibCapabilities.ITEM_PREVIEWABLE)
          new IPreviewable {
            /**
              *
              * @return The ID of IPreviewableRenderer.  This is separate from Forge RenderIDs.
              */
            override def renderID: Int = PreviewableIDs.testID

            override def snapToBlockGrid: Boolean = false
          }.asInstanceOf[T]
        else null.asInstanceOf[T]
      }

      override def hasCapability(capability: Capability[_], facing: EnumFacing): Boolean =
        capability == ItszuLibCapabilities.ITEM_PREVIEWABLE
    }
  }
}
