package com.itszuvalex.itszulib

import java.util.concurrent.ConcurrentHashMap

import com.itszuvalex.itszulib.api.core.{DimensionMapper, Loc4}
import com.itszuvalex.itszulib.api.wrappers.{IWorld, WrapperVanillaItemStack}
import org.scalamock.scalatest.MockFactory
import org.scalatest.{Matchers, OneInstancePerTest, WordSpec}

import scala.collection.JavaConverters._
import scala.collection.mutable
import scala.util.Random

/**
  * Created by Christopher Harris (Itszuvalex) on 4/14/15.
  */
object TestBase {
  val testableWorlds: mutable.Map[Int, IWorld] = new ConcurrentHashMap[Int, IWorld]().asScala

  def getRandomWorldId: Int = {
    var id = 0
    do {
      id = Random.nextInt()
    } while (testableWorlds.contains(id))
    id
  }
}

abstract class TestBase extends WordSpec with Matchers with OneInstancePerTest with MockFactory {
  WrapperVanillaItemStack.nbtSerializer.revert()
  Loc4.OverrideDimensionMapper = Some(new DimensionMapper {
    override def dimensionIdForWorld(w: IWorld): Int = w.dimensionId

    override def worldForDimensionId(i: Int): IWorld = TestBase.testableWorlds.get(i).orNull
  })

}

