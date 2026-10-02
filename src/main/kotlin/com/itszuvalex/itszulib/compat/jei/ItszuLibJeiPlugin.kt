package com.itszuvalex.itszulib.compat.jei

import com.itszuvalex.itszulib.ItszuLib
import com.itszuvalex.itszulib.client.screen.ComponentScreen
import mezz.jei.api.IModPlugin
import mezz.jei.api.JeiPlugin
import mezz.jei.api.gui.handlers.IGuiContainerHandler
import mezz.jei.api.registration.IGuiHandlerRegistration
import net.minecraft.client.renderer.Rect2i
import net.minecraft.resources.Identifier

/**
 * Optional JEI support: tells JEI where every [ComponentScreen] draws outside its image (side panel tabs and the open
 * panel, [ComponentScreen.extraAreas]), so JEI's ingredient list moves out of the way instead of covering them. JEI
 * finds this class through [JeiPlugin]; nothing else references it, so it only loads when JEI is installed.
 */
@JeiPlugin
class ItszuLibJeiPlugin : IModPlugin {
    override fun getPluginUid(): Identifier = Identifier.fromNamespaceAndPath(ItszuLib.ID, "main")

    override fun registerGuiHandlers(registration: IGuiHandlerRegistration) {
        registration.addGenericGuiContainerHandler(ComponentScreen::class.java, object : IGuiContainerHandler<ComponentScreen<*>> {
            override fun getGuiExtraAreas(screen: ComponentScreen<*>): List<Rect2i> = screen.extraAreas()
        })
    }
}
