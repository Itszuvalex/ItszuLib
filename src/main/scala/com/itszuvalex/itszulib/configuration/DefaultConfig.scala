package com.itszuvalex.itszulib.configuration

/**
  * Created by Chris on 1/22/2017.
  */
class DefaultConfig extends IConfig {
  override def get[T](name: String, default: T): T = get[T](name, default, null)

  override def get[T](name: String, default: T, description: String): T = default
}
