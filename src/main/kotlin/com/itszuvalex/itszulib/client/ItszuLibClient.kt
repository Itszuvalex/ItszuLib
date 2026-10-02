package com.itszuvalex.itszulib.client

import com.itszuvalex.itszulib.client.scene.BlockSceneRenderer
import com.itszuvalex.itszulib.client.screen.ScreenThemeConfig
import com.itszuvalex.itszulib.client.screen.ScreenThemes
import net.neoforged.bus.api.IEventBus
import net.neoforged.fml.ModLoadingContext
import net.neoforged.fml.config.ModConfig
import net.neoforged.neoforge.client.event.AddClientReloadListenersEvent
import net.neoforged.neoforge.client.gui.ConfigurationScreen
import net.neoforged.neoforge.client.gui.IConfigScreenFactory

/**
 * ItszuLib's client registrations (dev and production). Only referenced when running on a client.
 */
object ItszuLibClient {
    fun register(modBus: IEventBus) {
        modBus.addListener(BlockSceneRenderer::register)
        modBus.addListener { event: AddClientReloadListenersEvent -> event.addListener(ScreenThemes.LOADER_ID, ScreenThemes.Loader()) }
        val container = ModLoadingContext.get().activeContainer
        container.registerConfig(ModConfig.Type.CLIENT, ScreenThemeConfig.SPEC)
        container.registerExtensionPoint(IConfigScreenFactory::class.java) { IConfigScreenFactory { container, parent -> ConfigurationScreen(container, parent) } }
    }
}
