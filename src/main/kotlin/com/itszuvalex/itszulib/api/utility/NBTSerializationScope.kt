package com.itszuvalex.itszulib.api.utility

/**
 * Which kind of persistence a fragment's data takes part in.
 * - [LEVEL]: world save.
 * - [DESCRIPTION]: client sync (chunk load and block update packets).
 * - [ITEM]: block entity data carried on the dropped/picked item.
 */
enum class NBTSerializationScope {
    ITEM,
    DESCRIPTION,
    LEVEL,
}
