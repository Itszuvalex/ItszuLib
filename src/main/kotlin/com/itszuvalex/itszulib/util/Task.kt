package com.itszuvalex.itszulib.util

import com.itszuvalex.itszulib.api.adapters.IBattery
import net.minecraft.world.level.storage.ValueInput
import net.minecraft.world.level.storage.ValueOutput
import net.neoforged.neoforge.common.util.ValueIOSerializable
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

/**
 * A job that needs [baseGoal] energy, spread over at least [minTicks] ticks. Port of ItszuLib 1.12.2's `Task`.
 *
 * Speed and efficiency are modifiers around 0: positive speed shortens the job (`minTicks / (1 + speed)`), negative
 * lengthens it (`minTicks * (1 - speed)`); efficiency scales the energy needed the same way.
 *
 * Differences from 1.12.2: a job always takes at least one tick (with `minTicks` 0 and `baseGoal` 0, 1.12.2 divided 0
 * by 0 and the progress became NaN, so the job never completed), and negative power contributes nothing (it used to
 * undo progress).
 */
open class Task @JvmOverloads constructor(var baseGoal: Double = 0.0, var minTicks: Int = 0) : ValueIOSerializable {
    var progress: Double = 0.0

    /**
     * Energy per tick needed to finish in [adjustedTicks].
     */
    fun powerPerTick(speed: Double, efficiency: Double): Double = adjustedMax(efficiency) / adjustedTicks(speed)

    /**
     * Ticks the job takes at [speed]; at least 1.
     */
    fun adjustedTicks(speed: Double): Int = max(1, ceil(scale(minTicks.toDouble(), speed)).toInt())

    /**
     * Energy the job needs at [efficiency].
     */
    fun adjustedMax(efficiency: Double): Double = scale(baseGoal, efficiency)

    /**
     * Adds up to one tick's worth of [power].
     *
     * @return Energy used, out of [power].
     */
    open fun contribute(power: Double, speed: Double, efficiency: Double): Double {
        val take = min(progressRemaining(efficiency), powerPerTick(speed, efficiency))
        val used = max(0.0, min(power, take))
        progress += used
        return used
    }

    /**
     * Draws up to one tick's worth of energy from [battery] (port of Femtocraft 1.12.2's `BatteryPoweredTask`).
     *
     * @return Energy used.
     */
    fun contributeFrom(battery: IBattery, speed: Double, efficiency: Double): Double {
        val used = contribute(min(powerPerTick(speed, efficiency), battery.storage()), speed, efficiency)
        if (used > 0) battery.drain(used)
        return used
    }

    fun progressRemaining(efficiency: Double): Double = max(adjustedMax(efficiency) - progress, 0.0)

    fun completed(efficiency: Double): Boolean = progressRemaining(efficiency) <= 0

    /**
     * Progress as a fraction of the energy needed at [efficiency], in [0, 1].
     */
    fun fraction(efficiency: Double): Double {
        val goal = adjustedMax(efficiency)
        return if (goal <= 0) 1.0 else (progress / goal).coerceIn(0.0, 1.0)
    }

    fun reset() {
        progress = 0.0
    }

    override fun serialize(output: ValueOutput) {
        output.putDouble(PROGRESS_KEY, progress)
        output.putDouble(GOAL_KEY, baseGoal)
        output.putInt(TICKS_KEY, minTicks)
    }

    override fun deserialize(input: ValueInput) {
        progress = input.getDoubleOr(PROGRESS_KEY, 0.0)
        baseGoal = input.getDoubleOr(GOAL_KEY, baseGoal)
        minTicks = input.getIntOr(TICKS_KEY, minTicks)
    }

    companion object {
        const val PROGRESS_KEY = "Progress"
        const val GOAL_KEY = "Goal"
        const val TICKS_KEY = "Ticks"

        /**
         * [value] divided by `1 + modifier` for a positive modifier, multiplied by `1 - modifier` for a negative one.
         */
        private fun scale(value: Double, modifier: Double): Double = when {
            modifier > 0 -> value / (1.0 + modifier)
            modifier < 0 -> value * (1.0 - modifier)
            else -> value
        }
    }
}
