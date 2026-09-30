package com.itszuvalex.itszulib.api.utility

import net.minecraft.world.level.storage.ValueInput
import net.minecraft.world.level.storage.ValueOutput

interface IScopedSerialization {
    fun serializeTo(scope: NBTSerializationScope, output: ValueOutput)

    fun deserialize(input: ValueInput, scope: NBTSerializationScope)

    fun handlesScope(scope: NBTSerializationScope): Boolean
}
