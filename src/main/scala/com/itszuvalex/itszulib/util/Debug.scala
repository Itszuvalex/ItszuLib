package com.itszuvalex.itszulib.util

import com.itszuvalex.itszulib.ItszuLib
import org.apache.logging.log4j.Level

/**
  * Created by Chris on 12/18/2016.
  */
object Debug {
  val ENV_DEBUG = "DEBUG"
  lazy val isDebug: Boolean = try {System.getenv(ENV_DEBUG).toBoolean} catch {case _: Throwable => false}

  def only(func: => Unit): Unit = {
    try {
      if (isDebug)
        func
    }
    catch {
      case _: Throwable =>
    }
  }

  def assert(func: => Boolean, msg: String): Unit = {
    only {
      try {
        scala.Predef.assert(func)
      }
      catch {
        case a: Throwable => ItszuLib.logger.log(Level.ERROR, msg, a)
      }
    }
  }

  def assertNotNull(obj: AnyRef, msg: String): Unit = assert(obj != null, msg)

  def log(level: Level, msg: String): Unit = only(ItszuLib.logger.log(level, msg))

}
