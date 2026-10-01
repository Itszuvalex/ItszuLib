package com.itszuvalex.itszulib.dev

import com.itszuvalex.itszulib.client.ScreenHelpers
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
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
 * A plain screen for [DevMenu]: grey panel, slot outlines, and the machine's tank (with a tooltip).
 */
class DevScreen(menu: DevMenu, inventory: Inventory, title: Component) : AbstractContainerScreen<DevMenu>(menu, inventory, title) {
    override fun extractBackground(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, a: Float) {
        super.extractBackground(graphics, mouseX, mouseY, a)
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, PANEL)
        for (slot in menu.slots) graphics.fill(leftPos + slot.x - 1, topPos + slot.y - 1, leftPos + slot.x + 17, topPos + slot.y + 17, SLOT)
        val machine = menu.blockEntity as? DevMachineBlockEntity ?: return
        graphics.fill(leftPos + TANK_X - 1, topPos + TANK_Y - 1, leftPos + TANK_X + TANK_W + 1, topPos + TANK_Y + TANK_H + 1, SLOT)
        ScreenHelpers.fluidTank(graphics, leftPos + TANK_X, topPos + TANK_Y, TANK_W, TANK_H, machine.tanks.get(0).toMinecraft(), DevMachineBlockEntity.TANK_CAPACITY)
        ScreenHelpers.tooltipIfHovered(
            graphics, mouseX, mouseY, leftPos + TANK_X, topPos + TANK_Y, TANK_W, TANK_H,
            ScreenHelpers.fluidTooltip(machine.tanks.get(0).toMinecraft(), DevMachineBlockEntity.TANK_CAPACITY),
        )
    }

    companion object {
        private const val PANEL = 0xFFC6C6C6.toInt()
        private const val SLOT = 0xFF8B8B8B.toInt()
        private const val TANK_X = 152
        private const val TANK_Y = 8
        private const val TANK_W = 16
        private const val TANK_H = 60
    }
}
