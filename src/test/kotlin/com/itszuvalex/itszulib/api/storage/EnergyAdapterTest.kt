package com.itszuvalex.itszulib.api.storage

import com.itszuvalex.itszulib.api.wrappers.WrapperEnergyHandlerIBattery
import net.neoforged.neoforge.transfer.energy.SimpleEnergyHandler
import net.neoforged.neoforge.transfer.transaction.Transaction
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class EnergyAdapterTest {
    @Test
    fun WrapperEnergyHandler_AbortedTransactionRollsBack() {
        val battery = PowerBattery(100.0)
        val handler = WrapperEnergyHandlerIBattery(battery)
        Transaction.openRoot().use { tx ->
            Assertions.assertEquals(30, handler.insert(30, tx))
            Assertions.assertEquals(30.0, battery.storage())
        }
        Assertions.assertEquals(0.0, battery.storage())
    }

    @Test
    fun WrapperEnergyHandler_CommittedTransactionPersistsAndClamps() {
        val battery = PowerBattery(100.0)
        val handler = WrapperEnergyHandlerIBattery(battery)
        Transaction.openRoot().use { tx ->
            Assertions.assertEquals(100, handler.insert(150, tx))
            Assertions.assertEquals(40, handler.extract(40, tx))
            tx.commit()
        }
        Assertions.assertEquals(60.0, battery.storage())
        Assertions.assertEquals(60, handler.amountAsLong)
        Assertions.assertEquals(100, handler.capacityAsLong)
    }

    @Test
    fun WrapperEnergyHandler_NotifiesBatteryOnceOnCommitOnly() {
        var changes = 0
        val battery = PowerBattery(100.0) { changes++ }
        val handler = WrapperEnergyHandlerIBattery(battery)
        Transaction.openRoot().use { tx -> handler.insert(30, tx) }
        Assertions.assertEquals(0, changes, "aborted transaction must not notify")
        Transaction.openRoot().use { tx ->
            handler.insert(30, tx)
            handler.extract(10, tx)
            Assertions.assertEquals(0, changes, "no notification while the transaction is open")
            tx.commit()
        }
        Assertions.assertEquals(1, changes)
    }

    @Test
    fun IBattery_FillDrainNegative_DoNothing() {
        val battery = PowerBattery(100.0)
        battery.setStorage(50.0)
        Assertions.assertEquals(0.0, battery.fill(-10.0))
        Assertions.assertEquals(0.0, battery.drain(-10.0))
        Assertions.assertEquals(50.0, battery.storage())
    }

    @Test
    fun WrapperEnergyHandler_TruncatesFractionalEnergy() {
        val battery = PowerBattery(100.0)
        battery.setStorage(10.75)
        val handler = WrapperEnergyHandlerIBattery(battery)
        val extracted = Transaction.openRoot().use { tx -> handler.extract(50, tx).also { tx.commit() } }
        Assertions.assertEquals(10, extracted)
        Assertions.assertEquals(0.75, battery.storage(), 1e-9)
    }

    @Test
    fun BatteryEnergyHandler_FillAndDrainThroughHandler() {
        val battery = BatteryEnergyHandler(SimpleEnergyHandler(100))
        Assertions.assertEquals(100.0, battery.maxStorage())
        Assertions.assertEquals(80.0, battery.fill(80.9))
        Assertions.assertEquals(20.0, battery.room())
        Assertions.assertEquals(20.0, battery.fill(50.0))
        Assertions.assertEquals(30.0, battery.drain(30.0))
        battery.setStorage(10.0)
        Assertions.assertEquals(10.0, battery.storage())
    }

    // 1.12.2's PowerBattery clamped its charge to the capacity; the port did not, so a save from a larger battery (or a
    // direct setStorage) left it over capacity with negative room.
    @Test
    fun PowerBattery_SetStorage_ClampsToCapacity() {
        val battery = PowerBattery(100.0)
        battery.setStorage(150.0)
        Assertions.assertEquals(100.0, battery.storage())
        battery.setStorage(-5.0)
        Assertions.assertEquals(0.0, battery.storage())
    }

    @Test
    fun PowerBattery_Deserialize_OverCapacity_Clamped() {
        val tag = com.itszuvalex.itszulib.TestIO.write { PowerBattery(500.0).also { b -> b.setStorage(400.0) }.serialize(it) }
        val battery = PowerBattery(100.0)
        battery.deserialize(com.itszuvalex.itszulib.TestIO.read(tag))
        Assertions.assertEquals(100.0, battery.storage())
        Assertions.assertEquals(0.0, battery.room())
    }
}
