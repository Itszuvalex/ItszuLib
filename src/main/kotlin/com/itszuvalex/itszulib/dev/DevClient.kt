package com.itszuvalex.itszulib.dev

import com.itszuvalex.itszulib.client.screen.ComponentScreen
import com.itszuvalex.itszulib.client.screen.EnergyGauge
import com.itszuvalex.itszulib.client.screen.FluidGauge
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Inventory
import net.neoforged.bus.api.IEventBus
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent

/**
 * Client-only dev registrations. Only referenced when running on a client.
 */
object DevClient {
    fun register(modBus: IEventBus) {
        modBus.addListener(::registerScreens)
    }

    private fun registerScreens(event: RegisterMenuScreensEvent) {
        event.register(DevContent.DEV_MENU.get(), ::DevScreen)
    }
}

/**
 * [DevMenu]'s screen, built from ItszuLib's screen components: the machine's tank and energy gauges, and (from the
 * menu's side configuration) the 3D side configuration panel behind the "IO" tab.
 */
class DevScreen(menu: DevMenu, inventory: Inventory, title: Component) : ComponentScreen<DevMenu>(menu, inventory, title) {
    override fun addComponents() {
        val machine = menu.blockEntity as? DevMachineBlockEntity ?: return
        addComponent(FluidGauge({ machine.tanks.get(0).toMinecraft() }, { DevMachineBlockEntity.TANK_CAPACITY }, 16, 60), 152, 8)
        addComponent(EnergyGauge({ menu.energy }), 10, 16)
    }
}
