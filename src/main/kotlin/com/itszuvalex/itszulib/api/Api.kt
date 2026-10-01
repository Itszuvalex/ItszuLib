package com.itszuvalex.itszulib.api

import com.itszuvalex.itszulib.ItszuLib
import com.itszuvalex.itszulib.api.adapters.IColorable
import com.itszuvalex.itszulib.api.adapters.IModule
import com.itszuvalex.itszulib.api.adapters.Module
import com.itszuvalex.itszulib.api.multiblock.MultiBlockInfo
import com.itszuvalex.itszulib.api.storage.IFluidStorage
import com.itszuvalex.itszulib.api.storage.IItemStorage
import com.itszuvalex.itszulib.core.BlockEntityCore
import com.itszuvalex.itszulib.menu.IMenuHost
import com.itszuvalex.itszulib.core.SidedFluidStorageConfiguration
import com.itszuvalex.itszulib.core.SidedItemStorageConfiguration
import net.minecraft.core.Direction
import net.minecraft.core.component.DataComponentType
import net.minecraft.core.registries.Registries
import net.minecraft.resources.Identifier
import net.minecraft.world.item.component.CustomData
import net.minecraft.world.level.block.entity.BlockEntityType
import net.neoforged.bus.api.IEventBus
import net.neoforged.neoforge.capabilities.BlockCapability
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent
import net.neoforged.neoforge.registries.DeferredHolder
import net.neoforged.neoforge.registries.DeferredRegister
import net.neoforged.neoforge.capabilities.Capabilities as NeoCapabilities

/**
 * ItszuLib block capabilities.
 */
object Capabilities {
    @JvmField
    val COLORABLE: BlockCapability<IColorable, Direction?> =
        BlockCapability.createSided(Identifier.fromNamespaceAndPath(ItszuLib.ID, "colorable"), IColorable::class.java)
}

/**
 * Built-in modules. Loaded (and so registered) by [init] during mod construction, before RegisterCapabilitiesEvent.
 */
object Modules {
    @JvmField
    val COLORABLE: IModule<IColorable> =
        Module.registerModule(Identifier.fromNamespaceAndPath(ItszuLib.ID, "colorable"), Capabilities.COLORABLE)

    /**
     * A block entity's item storage, per side. Exposed by [com.itszuvalex.itszulib.core.frag.FragItemStorage], which
     * also exposes NeoForge's item capability.
     */
    @JvmField
    val ITEM_STORAGE: IModule<IItemStorage> = Module.registerModule(id("item_storage"), null)

    /**
     * A block entity's fluid storage, per side. Exposed by [com.itszuvalex.itszulib.core.frag.FragFluidStorage], which
     * also exposes NeoForge's fluid capability.
     */
    @JvmField
    val FLUID_STORAGE: IModule<IFluidStorage> = Module.registerModule(id("fluid_storage"), null)

    /**
     * Which item storage and automatic IO mode each face uses. When present, [ITEM_STORAGE] follows it per side.
     */
    @JvmField
    val ITEM_STORAGE_CONFIGURABLE: IModule<SidedItemStorageConfiguration> =
        Module.registerModule(id("item_storage_configurable"), null)

    /**
     * Which fluid storage and automatic IO mode each face uses. When present, [FLUID_STORAGE] follows it per side.
     */
    @JvmField
    val FLUID_STORAGE_CONFIGURABLE: IModule<SidedFluidStorageConfiguration> =
        Module.registerModule(id("fluid_storage_configurable"), null)

    /**
     * Multiblock membership of a block entity.
     */
    @JvmField
    val MULTIBLOCK: IModule<MultiBlockInfo> = Module.registerModule(id("multiblock"), null)

    /**
     * The menu a block opens when used, per clicked face. Exposed by [com.itszuvalex.itszulib.core.frag.FragMenu];
     * [com.itszuvalex.itszulib.core.EntityBlockCore] opens it.
     */
    @JvmField
    val MENU: IModule<IMenuHost> = Module.registerModule(id("menu"), null)

    private fun id(path: String): Identifier = Identifier.fromNamespaceAndPath(ItszuLib.ID, path)

    /**
     * Forces the built-in modules above to register.
     */
    @JvmStatic
    fun init() {}
}

/**
 * ItszuLib data component types.
 */
object Components {
    @JvmField
    val DATA_COMPONENTS: DeferredRegister.DataComponents =
        DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, ItszuLib.ID)

    /**
     * ITEM-scope fragment data of a [BlockEntityCore], carried by its item form. Written by
     * `collectImplicitComponents` (loot `copy_components` from the block entity, creative pick-block) and applied back
     * on placement.
     */
    @JvmField
    val FRAGMENT_DATA: DeferredHolder<DataComponentType<*>, DataComponentType<CustomData>> =
        DATA_COMPONENTS.registerComponentType("fragment_data") { builder ->
            builder.persistent(CustomData.CODEC).networkSynchronized(CustomData.STREAM_CODEC)
        }

    @JvmStatic
    fun register(modEventBus: IEventBus) = DATA_COMPONENTS.register(modEventBus)
}

/**
 * Exposes [BlockEntityCore] fragments to the NeoForge capability system.
 */
object ModuleCapabilities {
    /**
     * NeoForge block capabilities that block entities may expose through
     * [com.itszuvalex.itszulib.core.BlockEntityFragmentCollection.addCapability].
     */
    @JvmField
    val STANDARD: List<BlockCapability<*, Direction?>> = listOf(
        NeoCapabilities.Item.BLOCK,
        NeoCapabilities.Fluid.BLOCK,
        NeoCapabilities.Energy.BLOCK,
    )

    /**
     * Call from a [RegisterCapabilitiesEvent] handler (mod bus) for each [BlockEntityCore] type. Registers every
     * module's block capability plus [STANDARD], all routed to [BlockEntityCore.getCapability]. Modules must already
     * be registered.
     */
    @JvmStatic
    fun registerBlockEntity(event: RegisterCapabilitiesEvent, type: BlockEntityType<out BlockEntityCore>) {
        val caps = LinkedHashSet<BlockCapability<*, Direction?>>()
        Module.modules().forEach { m -> m.blockCapability?.let(caps::add) }
        caps.addAll(STANDARD)
        caps.forEach { register(event, it, type) }
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T : Any> register(event: RegisterCapabilitiesEvent, cap: BlockCapability<T, Direction?>, type: BlockEntityType<out BlockEntityCore>) {
        event.registerBlockEntity(cap, type as BlockEntityType<BlockEntityCore>) { be, side -> be.getCapability(cap, side) }
    }
}
