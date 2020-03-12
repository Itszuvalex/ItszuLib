package com.itszuvalex.itszulib.api.core
import com.itszuvalex.itszulib.api.wrappers.IWorld

trait DimensionMapper {

  def dimensionIdForWorld(w: IWorld): Int

  def worldForDimensionId(i: Int): IWorld
}
