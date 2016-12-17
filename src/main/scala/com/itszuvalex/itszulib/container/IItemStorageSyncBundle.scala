package com.itszuvalex.itszulib.container

import com.itszuvalex.itszulib.api.storage.IItemStorage
import com.itszuvalex.itszulib.api.wrappers.IItemStack
import com.itszuvalex.itszulib.container.sync.SyncIItemStack

/**
  * Created by Chris on 12/17/2016.
  */
class IItemStorageSyncBundle(container: ContainerBase, storage: IItemStorage) {
  storage.indices.foreach { i =>
    container.addSync(new SyncIItemStack(() => storage(i), (item: IItemStack) => storage(i) = item))
  }
}
