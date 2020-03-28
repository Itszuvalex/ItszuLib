package com.itszuvalex.itszulib.api.utility

import net.minecraft.util.EnumFacing

/**
  * Created by Chris on 7/31/2016.
  */
object FacingUtil {
  val DEFAULT_HORIZONTAL_FACING = EnumFacing.NORTH

  def getAbsoluteFacingFromHorizontalRelative(relativeFacing: EnumFacing, relativeFront: EnumFacing): EnumFacing = {
    // Swizzled from input for readability
    (relativeFront, relativeFacing) match {
      case (_, EnumFacing.UP) => EnumFacing.UP
      case (_, EnumFacing.DOWN) => EnumFacing.DOWN
      // Looking North - no modification
      case (EnumFacing.NORTH, a) => a
      // Looking East
      case (EnumFacing.EAST, EnumFacing.NORTH) => EnumFacing.EAST
      case (EnumFacing.EAST, EnumFacing.EAST) => EnumFacing.SOUTH
      case (EnumFacing.EAST, EnumFacing.SOUTH) => EnumFacing.WEST
      case (EnumFacing.EAST, EnumFacing.WEST) => EnumFacing.NORTH
      // Looking South
      case (EnumFacing.SOUTH, EnumFacing.NORTH) => EnumFacing.SOUTH
      case (EnumFacing.SOUTH, EnumFacing.EAST) => EnumFacing.WEST
      case (EnumFacing.SOUTH, EnumFacing.SOUTH) => EnumFacing.NORTH
      case (EnumFacing.SOUTH, EnumFacing.WEST) => EnumFacing.EAST
      // Looking West
      case (EnumFacing.WEST, EnumFacing.NORTH) => EnumFacing.WEST
      case (EnumFacing.WEST, EnumFacing.EAST) => EnumFacing.NORTH
      case (EnumFacing.WEST, EnumFacing.SOUTH) => EnumFacing.EAST
      case (EnumFacing.WEST, EnumFacing.WEST) => EnumFacing.SOUTH
      case (_, a) => a
    }
  }

  def getHorizontalRelativeFacingFromAbsolute(absoluteFacing: EnumFacing, relativeFront: EnumFacing): EnumFacing = {
    // Swizzled from input for readability
    (relativeFront, absoluteFacing) match {
      case (_, EnumFacing.UP) => EnumFacing.UP
      case (_, EnumFacing.DOWN) => EnumFacing.DOWN
      // Looking North - no modification
      case (EnumFacing.NORTH, a) => a
      // Looking East
      case (EnumFacing.EAST, EnumFacing.NORTH) => EnumFacing.WEST
      case (EnumFacing.EAST, EnumFacing.EAST) => EnumFacing.NORTH
      case (EnumFacing.EAST, EnumFacing.SOUTH) => EnumFacing.EAST
      case (EnumFacing.EAST, EnumFacing.WEST) => EnumFacing.SOUTH
      // Looking South
      case (EnumFacing.SOUTH, EnumFacing.NORTH) => EnumFacing.SOUTH
      case (EnumFacing.SOUTH, EnumFacing.EAST) => EnumFacing.WEST
      case (EnumFacing.SOUTH, EnumFacing.SOUTH) => EnumFacing.NORTH
      case (EnumFacing.SOUTH, EnumFacing.WEST) => EnumFacing.EAST
      // Looking West
      case (EnumFacing.WEST, EnumFacing.NORTH) => EnumFacing.EAST
      case (EnumFacing.WEST, EnumFacing.EAST) => EnumFacing.SOUTH
      case (EnumFacing.WEST, EnumFacing.SOUTH) => EnumFacing.WEST
      case (EnumFacing.WEST, EnumFacing.WEST) => EnumFacing.NORTH
      case (_, a) => a
    }
  }
}
