package com.itszuvalex.itszulib

import com.itszuvalex.itszulib.api.core.Loc4
import com.itszuvalex.itszulib.api.wrappers.{IItemStack, WrapperVanillaItemStack}
import org.scalamock.scalatest.MockFactory
import org.scalatest.{Matchers, OneInstancePerTest, WordSpec}

/**
  * Created by Christopher Harris (Itszuvalex) on 4/14/15.
  */
abstract class TestBase extends WordSpec with Matchers with OneInstancePerTest with MockFactory {
  IItemStack.nbtLoader.revert()
  WrapperVanillaItemStack.nbtSerializer.revert()
  Loc4.intWorldMapper.revert()
}

