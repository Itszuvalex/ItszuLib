package com.itszuvalex.itszulib.core.frag

import com.itszuvalex.itszulib.api.adapters.ILevel
import com.itszuvalex.itszulib.api.utility.NBTSerializationScope
import com.itszuvalex.itszulib.core.IBlockEntityFragment
import com.itszuvalex.itszulib.core.IFragmentHost
import com.itszuvalex.itszulib.core.IInternalBlockEntityFragment
import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.storage.ValueInput
import net.minecraft.world.level.storage.ValueOutput

/**
 * No-op defaults for fragments, plus access to the owning block entity once attached.
 */
abstract class InternalBlockEntityFragment : IInternalBlockEntityFragment {
    protected var host: IFragmentHost? = null
        private set

    override fun onAttach(host: IFragmentHost) {
        this.host = host
    }

    /**
     * [IFragmentHost.markDirty]; does nothing before the fragment is attached.
     */
    protected fun markDirty() {
        host?.markDirty()
    }

    /**
     * [IFragmentHost.markDirtyAndSync]; does nothing before the fragment is attached.
     */
    protected fun markDirtyAndSync() {
        host?.markDirtyAndSync()
    }

    override fun serializeTo(scope: NBTSerializationScope, output: ValueOutput) {}

    override fun deserialize(input: ValueInput, scope: NBTSerializationScope) {}

    override fun handlesScope(scope: NBTSerializationScope): Boolean = false

    override fun onRemove(level: ILevel, pos: BlockPos, blockStatePrev: BlockState) {}

    override fun invalidateFrags() {}

    override fun rehydrateFrags() {}
}

abstract class BlockEntityFragment<T : Any> : InternalBlockEntityFragment(), IBlockEntityFragment<T>
