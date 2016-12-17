package com.itszuvalex.itszulib.gui

import com.itszuvalex.itszulib.api.storage.IItemStorage
import com.itszuvalex.itszulib.api.wrappers.IItemStack

/**
  * Created by Chris on 12/17/2016.
  */
class GuiIItemStorageSlot(anchorX: Int, anchorY: Int, storage: IItemStorage, slot: Int, shouldRender: () => Boolean = () => true, str: String = null) extends
  GuiItemStack(anchorX, anchorY, shouldRender, str) {
  override def itemStack: IItemStack = storage(slot)
}
