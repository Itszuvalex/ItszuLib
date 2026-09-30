package com.itszuvalex.itszulib.dev

import com.itszuvalex.itszulib.ItszuLib
import com.itszuvalex.itszulib.api.ModuleCapabilities
import com.itszuvalex.itszulib.api.storage.IItemStorage
import com.itszuvalex.itszulib.api.storage.ItemStorageArray
import com.itszuvalex.itszulib.api.utility.NBTSerializationScope
import com.itszuvalex.itszulib.api.wrappers.WrapperResourceHandlerIItemStorage
import com.itszuvalex.itszulib.core.BlockEntityCore
import com.itszuvalex.itszulib.core.EntityBlockCore
import com.itszuvalex.itszulib.core.frag.FragColorable
import com.itszuvalex.itszulib.core.frag.FragDropInventory
import com.itszuvalex.itszulib.core.frag.InternalBlockEntityFragment
import net.minecraft.core.BlockPos
import net.minecraft.core.registries.Registries
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockBehaviour
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.storage.ValueInput
import net.minecraft.world.level.storage.ValueOutput
import net.neoforged.bus.api.IEventBus
import net.neoforged.neoforge.capabilities.Capabilities
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent
import net.neoforged.neoforge.registries.DeferredBlock
import net.neoforged.neoforge.registries.DeferredHolder
import net.neoforged.neoforge.registries.DeferredRegister

/**
 * Development-only content for exercising the framework in-game and in game tests. Never registered in production.
 */
object DevContent {
    @JvmField
    val BLOCKS: DeferredRegister.Blocks = DeferredRegister.createBlocks(ItszuLib.ID)

    @JvmField
    val BLOCK_ENTITY_TYPES: DeferredRegister<BlockEntityType<*>> = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, ItszuLib.ID)

    @JvmField
    val DEV_FRAG_BLOCK: DeferredBlock<DevFragBlock> = BLOCKS.registerBlock("dev_frag_block", ::DevFragBlock)

    @JvmField
    val DEV_FRAG_BLOCK_ENTITY: DeferredHolder<BlockEntityType<*>, BlockEntityType<DevFragBlockEntity>> =
        BLOCK_ENTITY_TYPES.register("dev_frag_block") { -> BlockEntityType(::DevFragBlockEntity, DEV_FRAG_BLOCK.get()) }

    fun register(modBus: IEventBus) {
        BLOCKS.register(modBus)
        BLOCK_ENTITY_TYPES.register(modBus)
        DevGameTests.register(modBus)
        modBus.addListener { event: RegisterCapabilitiesEvent ->
            ModuleCapabilities.registerBlockEntity(event, DEV_FRAG_BLOCK_ENTITY.get())
        }
    }
}

class DevFragBlock(properties: BlockBehaviour.Properties) :
    EntityBlockCore<DevFragBlockEntity>(properties, { DevContent.DEV_FRAG_BLOCK_ENTITY.get() }) {
    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity = DevFragBlockEntity(pos, state)
}

/**
 * A colorable block with a one-slot inventory that drops on removal and is exposed to other mods.
 */
class DevFragBlockEntity(pos: BlockPos, state: BlockState) : BlockEntityCore(DevContent.DEV_FRAG_BLOCK_ENTITY.get(), pos, state) {
    @JvmField
    val colorable = FragColorable()

    @JvmField
    val inventory: IItemStorage = ItemStorageArray(1) { markDirty() }

    @JvmField
    val itemHandler = WrapperResourceHandlerIItemStorage.of(inventory)

    init {
        fragList.addFragment(colorable)
        fragList.addInternalFragment(FragDropInventory(inventory))
        fragList.addInternalFragment(object : InternalBlockEntityFragment() {
            override fun name(): String = "Inventory"
            override fun handlesScope(scope: NBTSerializationScope): Boolean = scope == NBTSerializationScope.LEVEL
            override fun serializeTo(scope: NBTSerializationScope, output: ValueOutput) = inventory.serialize(output)
            override fun deserialize(input: ValueInput, scope: NBTSerializationScope) = inventory.deserialize(input)
        })
        fragList.addCapability(Capabilities.Item.BLOCK) { itemHandler }
    }
}
