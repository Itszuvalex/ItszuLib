package com.itszuvalex.itszulib.util

import net.minecraft.nbt.NBTTagCompound
import net.minecraftforge.common.util.INBTSerializable

/**
  * Created by Christopher Harris (Itszuvalex) on 7/25/16.
  */
object Task {
  val PROGRESS_KEY = "Progress"
  val GOAL_KEY     = "Goal"
  val TICKS_KEY    = "Ticks"
}

abstract class Task(var baseGoal: Double, var minTicks: Int) extends INBTSerializable[NBTTagCompound] {
  var progress: Double = 0

  def this() = this(0, 0)

  def powerPerTick(speed: Double, efficiency: Double): Double = adjustedMax(efficiency) / adjustedTicks(speed)

  def adjustedMax(efficiency: Double) = {
    efficiency match {
      case 0 => baseGoal
      case e if e > 0 => baseGoal / (1d + e)
      case e if e < 0 => baseGoal * (1d - e)
    }
  }

  def adjustedTicks(speed: Double) = {
    speed match {
      case 0 => minTicks
      case e if e > 0 => Math.ceil(minTicks / (1d + e)).toInt
      case e if e < 0 => Math.ceil(minTicks * (1d - e)).toInt
    }
  }

  /**
    *
    * @param power Power to contribute
    *
    * @return Power used out of power
    */
  def contribute(power: Double, speed: Double, efficiency: Double): Double = {
    val take = Math.min(progressRemaining(efficiency), powerPerTick(speed, efficiency))
    val ret = Math.min(power, take)
    progress += ret
    ret
  }

  def progressRemaining(efficiency: Double) = Math.max(adjustedMax(efficiency) - progress, 0)

  def completed(efficiency: Double) = progressRemaining(efficiency) <= 0

  def reset() = progress = 0

  override def deserializeNBT(t: NBTTagCompound): Unit = {
    progress = t.getDouble(Task.PROGRESS_KEY)
    baseGoal = t.getDouble(Task.GOAL_KEY)
    minTicks = t.getInteger(Task.TICKS_KEY)
  }

  override def serializeNBT(): NBTTagCompound = {
    val nbt = new NBTTagCompound()
    nbt.setDouble(Task.PROGRESS_KEY, progress)
    nbt.setDouble(Task.GOAL_KEY, baseGoal)
    nbt.setInteger(Task.TICKS_KEY, minTicks)
    nbt
  }
}
