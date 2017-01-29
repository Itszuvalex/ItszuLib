package com.itszuvalex.itszulib.container.sync

import com.itszuvalex.itszulib.network.ItszuLibPacketHandler
import com.itszuvalex.itszulib.network.messages.MessageSync
import com.itszuvalex.itszulib.util.Debug
import net.minecraft.entity.player.{EntityPlayer, EntityPlayerMP}
import org.apache.logging.log4j.Level

/**
  * Created by Chris on 12/13/2016.
  */
abstract class SyncBase[A](val gui: Int, valFunc: () => A, setValFunc: (A) => Unit, equalFunc: (A, A) => Boolean = (a: A, b: A) => (a == null && b == null) || Option(a).exists(_.equals(b))) extends ISync[A] {
  protected var _index         = 0
  protected var cachedValue: A = _
  private   var lastState      = 0

  var valueFunction      : () => A           = valFunc
  var valueSetFunction   : (A) => Unit       = setValFunc
  var valueEqualsFunction: (A, A) => Boolean = equalFunc

  override def state: Int = lastState

  override def updateState(state: Int): Unit = lastState = state

  override def GuiID: Int = gui

  override def syncIndex: Int = _index

  override def syncIndex_=(i: Int): Unit = _index = i

  override def value: A = cachedValue

  override def value_=(a: A): Unit = {
    cachedValue = a
    valueSetFunction(a)
  }

  def cache(a: A): A = a

  /**
    * Updates the cached value.
    *
    * @return True if should sync to players.
    */
  override def update(): Boolean = {
    val newVal = valueFunction()
    if (!valueEqualsFunction(value, newVal)) {
      cachedValue = cache(newVal)
      lastState += 1
      Debug.log(Level.WARN, "Updated.  New state = " + lastState)
      true
    } else false
  }

  override def sync(player: EntityPlayer): Unit = player match {
    case p: EntityPlayerMP => ItszuLibPacketHandler.INSTANCE.sendTo(new MessageSync(this), p)
    case _ =>
  }

  override def clear(): Unit = {
  }
}
