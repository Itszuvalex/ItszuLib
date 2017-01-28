package com.itszuvalex.itszulib.configuration

/**
  * Created by Chris on 1/22/2017.
  */
trait IConfig {

  def get[T](name: String, default: T): T

  def get[T](name: String, default: T, description: String): T

}
