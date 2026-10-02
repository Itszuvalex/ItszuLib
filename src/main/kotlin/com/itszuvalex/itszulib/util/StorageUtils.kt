package com.itszuvalex.itszulib.util

import com.itszuvalex.itszulib.api.adapters.IItemStack
import com.itszuvalex.itszulib.api.storage.IItemStorage

/**
 * Checks for and removes sets of items from an [IItemStorage]. Port of ItszuLib 1.12.2's `util/StorageUtils`.
 *
 * Difference from 1.12.2: only slots holding the requested item count towards it (1.12.2 never compared items, so any
 * items in the storage satisfied any request, and removing took whatever was there).
 */
object StorageUtils {
    /**
     * @return True if [storage] holds all of [items] (amounts summed per request, each slot used at most once).
     */
    @JvmStatic
    fun storageContainsItems(storage: IItemStorage, items: Iterable<IItemStack>): Boolean =
        removeItems(storage, items, doRemove = false, removeIfNotAllFound = false)

    /**
     * Removes [items] from [storage] if it holds all of them (or, with [removeIfNotAllFound], whatever it holds).
     *
     * @return True if all of [items] were found.
     */
    @JvmStatic
    @JvmOverloads
    fun removeItemsFromStorage(storage: IItemStorage, items: Iterable<IItemStack>, removeIfNotAllFound: Boolean = false): Boolean =
        removeItems(storage, items, doRemove = true, removeIfNotAllFound = removeIfNotAllFound)

    private fun removeItems(storage: IItemStorage, items: Iterable<IItemStack>, doRemove: Boolean, removeIfNotAllFound: Boolean): Boolean {
        val removed = IntArray(storage.size())
        var removedAll = true
        for (stack in items) {
            if (stack.isEmpty()) continue
            var toRemove = stack.stackSize()
            for (i in 0 until storage.size()) {
                if (toRemove <= 0) break
                val slot = storage.get(i)
                if (slot.isEmpty() || !slot.isItemEqual(stack)) continue
                val take = minOf(toRemove, slot.stackSize() - removed[i])
                if (take <= 0) continue
                removed[i] += take
                toRemove -= take
            }
            if (toRemove > 0) removedAll = false
        }
        if (doRemove && (removedAll || removeIfNotAllFound)) {
            for (i in removed.indices) if (removed[i] > 0) storage.split(i, removed[i])
        }
        return removedAll
    }
}
