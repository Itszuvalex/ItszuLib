package com.itszuvalex.itszulib

import com.itszuvalex.itszulib.dev.DevContent
import com.mojang.logging.LogUtils
import net.neoforged.fml.common.Mod
import net.neoforged.fml.loading.FMLEnvironment
import org.slf4j.Logger
import thedarkcolour.kotlinforforge.neoforge.forge.MOD_BUS

/**
 * ItszuLib mod entry point. Loaded by Kotlin for Forge (`modLoader="kotlinforforge"`), which instantiates this object.
 */
@Mod(ItszuLib.ID)
object ItszuLib {
    const val ID = "itszulib"
    const val NAME = "ItszuLib"

    @JvmField
    val LOGGER: Logger = LogUtils.getLogger()

    init {
        if (!FMLEnvironment.isProduction()) {
            DevContent.register(MOD_BUS)
        }
    }
}
