package com.itszuvalex.itszulib.container

import com.itszuvalex.itszulib.api.storage.IItemStorage
import com.itszuvalex.itszulib.container.sync.SyncItemStorageItemStack

/**
  * Created by Chris on 12/17/2016.
  */
class IItemStorageSyncBundle(gui: Int, container: ContainerBase, storage: IItemStorage, playerInv: Boolean) {
  storage.indices.foreach { i =>
    container.addSync(new SyncItemStorageItemStack(gui, storage, i, playerInv))
  }
}
