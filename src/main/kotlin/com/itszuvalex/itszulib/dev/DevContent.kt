package com.itszuvalex.itszulib.dev

import net.neoforged.bus.api.IEventBus

/**
 * Development-only content (test blocks, game tests). Registered only when `!FMLEnvironment.isProduction()`.
 */
object DevContent {
    fun register(modBus: IEventBus) {
        DevGameTests.register(modBus)
    }
}
