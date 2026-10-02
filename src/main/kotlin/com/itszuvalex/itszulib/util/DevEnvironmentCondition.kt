package com.itszuvalex.itszulib.util

import com.itszuvalex.itszulib.ItszuLib
import com.mojang.serialization.MapCodec
import net.neoforged.bus.api.IEventBus
import net.neoforged.fml.loading.FMLEnvironment
import net.neoforged.neoforge.common.conditions.ICondition
import net.neoforged.neoforge.registries.DeferredRegister
import net.neoforged.neoforge.registries.NeoForgeRegistries

/**
 * Load condition `itszulib:dev_environment`: true only in development runs (`!FMLEnvironment.isProduction()`), for
 * data that ships in the jar but should only load in dev, such as test technologies:
 * `"neoforge:conditions": [{ "type": "itszulib:dev_environment" }]`. Registered in production too, so such files are
 * skipped there instead of failing to parse.
 */
object DevEnvironmentCondition : ICondition {
    @JvmField
    val CODEC: MapCodec<DevEnvironmentCondition> = MapCodec.unit(this)

    private val CONDITIONS: DeferredRegister<MapCodec<out ICondition>> = DeferredRegister.create(NeoForgeRegistries.Keys.CONDITION_CODECS, ItszuLib.ID)

    init {
        CONDITIONS.register("dev_environment") { -> CODEC }
    }

    @JvmStatic
    fun register(modBus: IEventBus) = CONDITIONS.register(modBus)

    override fun test(context: ICondition.IContext): Boolean = !FMLEnvironment.isProduction()

    override fun codec(): MapCodec<out ICondition> = CODEC

    override fun toString(): String = "dev_environment"
}
