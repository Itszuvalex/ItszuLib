package com.itszuvalex.itszulib.container.sync

import com.itszuvalex.itszulib.network.ItszuLibPacketHandler
import com.itszuvalex.itszulib.network.messages.MessageSync
import net.minecraft.entity.player.{EntityPlayer, EntityPlayerMP}

/**
  * Created by Chris on 12/13/2016.
  */
abstract class SyncBase[A](valFunc: () => A, setValFunc: (A) => Unit, equalFunc: (A, A) => Boolean = (a: A, b: A) => (a == null && b == null) || Option(a).exists(_.equals(b))) extends ISync[A] {
  private var _index         = 0
  private var cachedValue: A = _

  val valueFunction      : () => A           = valFunc
  val valueSetFunction   : (A) => Unit       = setValFunc
  val valueEqualsFunction: (A, A) => Boolean = equalFunc

  override def index: Int = _index

  override def index_=(i: Int): Unit = _index = i

  override def value: A = cachedValue

  override def value_=(a: A): Unit = {
    cachedValue = a
    setValFunc(a)
  }

  /**
    * Updates the cached value.
    *
    * @return True if should sync to players.
    */
  override def update(): Boolean = {
    val newVal = valueFunction()
    if (!valueEqualsFunction(value, newVal)) {
      cachedValue = newVal
      true
    } else false
  }

  override def sync(player: EntityPlayer): Unit = player match {
    case p: EntityPlayerMP => ItszuLibPacketHandler.INSTANCE.sendTo(new MessageSync(this), p)
    case _ =>
  }
}
