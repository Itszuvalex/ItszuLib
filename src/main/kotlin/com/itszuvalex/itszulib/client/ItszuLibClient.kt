package com.itszuvalex.itszulib.client

import com.itszuvalex.itszulib.client.scene.BlockSceneRenderer
import net.neoforged.bus.api.IEventBus

/**
 * ItszuLib's client registrations (dev and production). Only referenced when running on a client.
 */
object ItszuLibClient {
    fun register(modBus: IEventBus) {
        modBus.addListener(BlockSceneRenderer::register)
    }
}
