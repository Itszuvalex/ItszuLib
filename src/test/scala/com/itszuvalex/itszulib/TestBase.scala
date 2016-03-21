package com.itszuvalex.itszulib

import org.scalamock.scalatest.MockFactory
import org.scalatest.{Matchers, OneInstancePerTest, WordSpec}

/**
  * Created by Christopher Harris (Itszuvalex) on 4/14/15.
  */
abstract class TestBase extends WordSpec with Matchers with OneInstancePerTest with MockFactory

